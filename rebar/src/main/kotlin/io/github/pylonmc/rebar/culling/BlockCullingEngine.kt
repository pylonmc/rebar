package io.github.pylonmc.rebar.culling

import com.destroystokyo.paper.event.block.BlockDestroyEvent
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.block.base.RebarCulledBlock
import io.github.pylonmc.rebar.block.base.RebarGroupCulledBlock
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.culling.PlayerCullingJob.Companion.cullingBoundingBox
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.util.Octree
import io.github.pylonmc.rebar.util.pdc
import io.github.pylonmc.rebar.util.position.BlockPosition
import io.github.pylonmc.rebar.util.rebarKey
import io.papermc.paper.event.world.border.WorldBorderBoundsChangeEvent
import io.papermc.paper.event.world.border.WorldBorderCenterChangeEvent
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldUnloadEvent
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import java.lang.invoke.MethodHandles
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.Map.Entry
import kotlin.collections.iterator
import kotlin.math.ceil

object BlockCullingEngine : Listener {

    val cullingEnabledKey = rebarKey("block_culling_enabled")
    val cullingConfigKey = rebarKey("block_culling_config")

    private val playerConfigCache = mutableMapOf<UUID, PlayerCullingConfig>()

    private val invertedVisibility = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer").let {
        MethodHandles.privateLookupIn(it, MethodHandles.lookup()).unreflectVarHandle(it.getDeclaredField("invertedVisibilityEntities"))
    }
    private val invertedVisibilityCache = mutableMapOf<UUID, MutableMap<UUID, *>>()

    internal val occludingCache = mutableMapOf<UUID, MutableMap<Long, ChunkData>>()

    internal val blockTextureOctrees = mutableMapOf<UUID, Octree<RebarBlock>>()
    internal  val culledBlockOctrees = mutableMapOf<UUID, Octree<RebarBlock>>()

    private val jobs = mutableMapOf<UUID, Job>()
    internal val syncJobTasks = ConcurrentHashMap<UUID, MutableMap<RebarCulledBlock, Boolean>>()
    internal val syncJobGroupTasks = ConcurrentHashMap<UUID, MutableMap<RebarGroupCulledBlock, MutableMap<RebarGroupCulledBlock.CullingGroup, Boolean>>>()

    /**
     * Periodically invalidates a share of the occluding cache, to ensure stale data isn't perpetuated.
     * Every [RebarConfig.CullingEngineConfig.OCCLUDING_CACHE_INVALIDATE_INTERVAL] ticks, it will invalidate [RebarConfig.CullingEngineConfig.OCCLUDING_CACHE_INVALIDATE_SHARE]
     * percent of the cache, starting with the oldest entries.
     *
     * Normally, blocks occluding state is cached the first time its requested, and is only updated when placed or broken.
     * If a block changes its occluding state in any other way the cache will no longer be accurate. This job corrects that.
     */
    @JvmSynthetic
    internal val invalidateOccludingCacheJob = Rebar.launch(start = CoroutineStart.LAZY) {
        while (true) {
            delay(RebarConfig.CullingEngineConfig.OCCLUDING_CACHE_INVALIDATE_INTERVAL.ticks)
            val now = System.currentTimeMillis()
            for ((worldId, chunkMap) in occludingCache) {
                var invalidated = 0
                var toInvalidate = ceil(chunkMap.size * RebarConfig.CullingEngineConfig.OCCLUDING_CACHE_INVALIDATE_SHARE)
                var entries = mutableListOf<Entry<Long, ChunkData>>()
                entries.addAll(chunkMap.entries)
                entries.sortBy { it.value.timestamp }

                for ((chunkKey, data) in entries) {
                    val world = Bukkit.getWorld(worldId) ?: continue
                    if (world.isChunkLoaded(chunkKey.toInt(), (chunkKey shr 32).toInt())) {
                        data.timestamp = now
                        data.occluding.cleanUp()
                        data.occluding.invalidateAll()
                        if (++invalidated >= toInvalidate) break
                    } else {
                        chunkMap.remove(chunkKey)
                    }
                }
            }
        }
    }

    @JvmSynthetic
    internal val syncCullingJob = Rebar.launch(start = CoroutineStart.LAZY) {
        var tick = 0
        while (true) {
            delay(RebarConfig.CullingEngineConfig.SYNC_APPLY_INTERVAL.ticks)

            for ((uuid, tasks) in syncJobTasks) {
                val player = Bukkit.getPlayer(uuid) ?: continue
                for ((block, shouldShow) in tasks) {
                    if (shouldShow) {
                        block.onVisible(player)
                    } else {
                        block.onCulled(player)
                    }
                }
                tasks.clear()
            }

            for ((uuid, groupTasks) in syncJobGroupTasks) {
                val player = Bukkit.getPlayer(uuid) ?: continue
                for ((block, tasks) in groupTasks) {
                    for ((group, shouldShow) in tasks) {
                        if (shouldShow) {
                            block.onGroupVisible(player, group)
                        } else {
                            block.onGroupCulled(player, group)
                        }
                    }
                }
                groupTasks.clear()
            }

            tick++
        }
    }

    @JvmSynthetic
    fun Player.isVisibilityInverted(entity: UUID): Boolean {
        val cache = invertedVisibilityCache.getOrPut(this.uniqueId) {
            @Suppress("UNCHECKED_CAST")
            invertedVisibility.get(this) as MutableMap<UUID, *>
        }
        return cache.containsKey(entity)
    }



    @JvmStatic
    var Player.hasBlockCulling: Boolean
        get() = (this.pdc.getOrDefault(cullingEnabledKey, RebarSerializers.BOOLEAN, RebarConfig.CullingEngineConfig.DEFAULT) || RebarConfig.CullingEngineConfig.FORCED_CULLING_PRESET != null) && !RebarConfig.CullingEngineConfig.FORCE_DISABLED
        set(value) {
            val actualValue = (value || RebarConfig.CullingEngineConfig.FORCED_CULLING_PRESET != null) && !RebarConfig.CullingEngineConfig.FORCE_DISABLED
            this.pdc.set(cullingEnabledKey, RebarSerializers.BOOLEAN, actualValue)
            if (!actualValue) {
                getOctree(this.world, culledBlockOctrees).query(cullingBoundingBox).forEach { block ->
                    (block as? RebarCulledBlock)?.onVisible(this)
                }
            }
        }

    @JvmStatic
    var Player.playerCullingConfig: PlayerCullingConfig
        get() = playerConfigCache.getOrPut(this.uniqueId) {
            if (RebarConfig.CullingEngineConfig.FORCED_CULLING_PRESET != null) {
                RebarConfig.CullingEngineConfig.FORCED_CULLING_PRESET.toPlayerConfig()
            } else {
                val config = pdc.getOrDefault(cullingConfigKey, RebarSerializers.PLAYER_CULLING_CONFIG, RebarConfig.CullingEngineConfig.DEFAULT_CULLING_PRESET.toPlayerConfig()).repairInvalid()
                if (RebarConfig.CullingEngineConfig.PRESETS_ONLY) {
                    val preset = RebarConfig.CullingEngineConfig.CULLING_PRESETS.values.firstOrNull { it.matches(config) }
                    if (preset == null) {
                        RebarConfig.CullingEngineConfig.DEFAULT_CULLING_PRESET.toPlayerConfig()
                    } else {
                        config
                    }
                } else {
                    config
                }
            }
        }
        set(value) {
            this.pdc.set(cullingConfigKey, RebarSerializers.PLAYER_CULLING_CONFIG, value)
            playerConfigCache[this.uniqueId] = value
        }

    @JvmSynthetic
    internal fun insert(block: RebarBlock) {
        if (!RebarConfig.CullingEngineConfig.ENABLED) return
        if (RebarConfig.BlockTextureConfig.ENABLED && !block.disableBlockTextureEntity) {
            getOctree(block.block.world, blockTextureOctrees).insert(block)
        }
        if (block is RebarCulledBlock) {
            getOctree(block.block.world, culledBlockOctrees).insert(block)
        }
    }

    @JvmSynthetic
    internal fun remove(block: RebarBlock) {
        if (!RebarConfig.CullingEngineConfig.ENABLED) return
        if (RebarConfig.BlockTextureConfig.ENABLED && !block.disableBlockTextureEntity) {
            getOctree(block.block.world, blockTextureOctrees).remove(block)
            block.blockTextureEntity?.removeAllViewers()
        }
        if (block is RebarCulledBlock) {
            getOctree(block.block.world, culledBlockOctrees).remove(block)
        }
    }

    val World.octreeBounds: BoundingBox
        get() {
            val border = this.worldBorder
            return BoundingBox.of(
                Vector(border.center.x - border.size / 2, this.minHeight.toDouble(), border.center.z - border.size / 2),
                Vector(border.center.x + border.size / 2, this.maxHeight.toDouble(), border.center.z + border.size / 2)
            )
        }

    @JvmSynthetic
    internal fun getOctree(world: World, octrees: MutableMap<UUID, Octree<RebarBlock>>): Octree<RebarBlock> {
        check(RebarConfig.CullingEngineConfig.ENABLED) { "Tried to access BlockCullingEngine octree while engine is disabled" }
        return octrees.getOrPut(world.uid) {
            Octree(
                bounds = world.octreeBounds,
                depth = 0,
                entryStrategy = { BoundingBox.of(it.block) },
                storeOutOfBoundsEntries = true
            )
        }
    }

    @JvmSynthetic
    internal fun launchCullingJob(player: Player) {
        val playerId = player.uniqueId
        if (!RebarConfig.CullingEngineConfig.ENABLED || jobs.containsKey(playerId)) return

        jobs[playerId] = Rebar.launch(
            context = Rebar.asyncDispatcher,
            block = PlayerCullingJob(playerId).job
        )
    }

    @JvmSynthetic
    internal fun stopCullingJob(playerId: UUID) {
        blockTextureOctrees.values.forEach { it.allEntries().forEach { b -> b.blockTextureEntity?.removeViewer(playerId) } }
        jobs.remove(playerId)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun onWorldLoad(event: WorldLoadEvent) {
        occludingCache[event.world.uid] = mutableMapOf()
        getOctree(event.world, blockTextureOctrees)
        getOctree(event.world, culledBlockOctrees)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onWorldBorderChangeSize(event: WorldBorderBoundsChangeEvent) {
        Bukkit.getScheduler().runTask(Rebar) { _ ->
            getOctree(event.world, blockTextureOctrees).resize(event.world.octreeBounds)
            getOctree(event.world, culledBlockOctrees).resize(event.world.octreeBounds)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onWorldBorderChangeCenter(event: WorldBorderCenterChangeEvent) {
        Bukkit.getScheduler().runTask(Rebar) { _ ->
            getOctree(event.world, blockTextureOctrees).resize(event.world.octreeBounds)
            getOctree(event.world, culledBlockOctrees).resize(event.world.octreeBounds)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun onPlayerJoin(event: PlayerJoinEvent) {
        launchCullingJob(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun onChunkLoad(event: ChunkLoadEvent) {
        occludingCache[event.world.uid]?.set(event.chunk.chunkKey, ChunkData())
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onBlockPlace(event: BlockPlaceEvent) {
        occludingCache[event.block.world.uid]?.get(event.block.chunk.chunkKey)?.insert(event.block)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onBlockBreak(event: BlockBreakEvent) {
        occludingCache[event.block.world.uid]?.get(event.block.chunk.chunkKey)?.insert(event.block, false)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onBlockDestroy(event: BlockDestroyEvent) {
        occludingCache[event.block.world.uid]?.get(event.block.chunk.chunkKey)?.insert(event.block, false)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onBlockExplode(event: BlockExplodeEvent) {
        val cache = occludingCache[event.block.world.uid] ?: return
        for (block in event.blockList()) {
            cache[block.chunk.chunkKey]?.insert(block, false)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onEntityExplode(event: EntityExplodeEvent) {
        val cache = occludingCache[event.entity.world.uid] ?: return
        for (block in event.blockList()) {
            cache[block.chunk.chunkKey]?.insert(block, false)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun onChunkUnload(event: ChunkUnloadEvent) {
        occludingCache[event.world.uid]?.remove(event.chunk.chunkKey)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun onWorldUnload(event: WorldUnloadEvent) {
        occludingCache.remove(event.world.uid)
        blockTextureOctrees.remove(event.world.uid)
        culledBlockOctrees.remove(event.world.uid)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun onPlayerQuit(event: PlayerQuitEvent) {
        jobs.remove(event.player.uniqueId)?.cancel()
        invertedVisibilityCache.remove(event.player.uniqueId)
        syncJobTasks.remove(event.player.uniqueId)
    }

    internal data class ChunkData(
        var timestamp: Long = System.currentTimeMillis(),
        val occluding: Cache<Long, Boolean> = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(1))
            .build()
    ) {
        fun insert(block: Block, isOccluding: Boolean = block.blockData.isOccluding) {
            occluding.put(BlockPosition.asLong(block.x, block.y, block.z), isOccluding)
        }

        fun isOccluding(world: World, blockX: Int, blockY: Int, blockZ: Int): Boolean {
            return occluding.get(BlockPosition.asLong(blockX, blockY, blockZ)) {
                world.getBlockAt(blockX, blockY, blockZ).blockData.isOccluding
            }
        }
    }
}
