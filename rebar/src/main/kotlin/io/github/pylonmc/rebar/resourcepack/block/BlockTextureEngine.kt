package io.github.pylonmc.rebar.resourcepack.block

import com.destroystokyo.paper.event.block.BlockDestroyEvent
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.block.base.RebarCulledBlock
import io.github.pylonmc.rebar.block.base.RebarGroupCulledBlock
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.event.RebarBlockBreakEvent
import io.github.pylonmc.rebar.util.Octree
import io.github.pylonmc.rebar.util.position.BlockPosition
import io.github.pylonmc.rebar.util.position.ChunkPosition
import io.github.pylonmc.rebar.util.rebarKey
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.bukkit.Bukkit
import org.bukkit.Chunk
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
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import java.lang.invoke.MethodHandles
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.Map.Entry
import kotlin.math.ceil

// TODO: Rename this because now we also do culling of entities not related to block textures
//  Also try to separate this so that culling of real entities can be enabled even when block texture entities are globally disabled
object BlockTextureEngine : Listener {
    const val DISABLED_PRESET = "disabled"

    val customBlockTexturesKey = rebarKey("custom_block_textures")
    val presetKey = rebarKey("culling_preset")

    private val invertedVisibility = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer").let {
        MethodHandles.privateLookupIn(it, MethodHandles.lookup()).unreflectVarHandle(it.getDeclaredField("invertedVisibilityEntities"))
    }
    private val invertedVisibilityCache = mutableMapOf<UUID, MutableMap<UUID, *>>()

    private val occludingCache = mutableMapOf<UUID, MutableMap<Long, ChunkData>>()

    private val blockTextureOctrees = mutableMapOf<UUID, Octree<RebarBlock>>()
    private val culledBlockOctrees = mutableMapOf<UUID, Octree<RebarBlock>>()
    private val jobs = mutableMapOf<UUID, Job>()
    private val syncJobTasks = ConcurrentHashMap<UUID, MutableMap<RebarCulledBlock, Boolean>>()
    private val syncJobGroupTasks = ConcurrentHashMap<UUID, MutableMap<RebarGroupCulledBlock, MutableMap<String, Boolean>>>()

    /**
     * Periodically invalidates a share of the occluding cache, to ensure stale data isn't perpetuated.
     * Every [RebarConfig.BlockTextureConfig.OCCLUDING_CACHE_REFRESH_INTERVAL] ticks, it will invalidate [RebarConfig.BlockTextureConfig.OCCLUDING_CACHE_REFRESH_INTERVAL]
     * percent of the cache, starting with the oldest entries.
     *
     * Normally, blocks occluding state is cached the first time its requested, and is only updated when placed or broken.
     * If a block changes its occluding state in any other way the cache will no longer be accurate. This job corrects that.
     */
    @JvmSynthetic
    internal val updateOccludingCacheJob = Rebar.launch(start = CoroutineStart.LAZY) {
        while (true) {
            delay(RebarConfig.BlockTextureConfig.OCCLUDING_CACHE_REFRESH_INTERVAL.ticks)
            val now = System.currentTimeMillis()
            for ((worldId, chunkMap) in occludingCache) {
                var refreshed = 0
                var toRefresh = ceil(chunkMap.size * RebarConfig.BlockTextureConfig.OCCLUDING_CACHE_REFRESH_SHARE)
                var entries = mutableListOf<Entry<Long, ChunkData>>()
                entries.addAll(chunkMap.entries)
                entries.sortBy { it.value.timestamp }

                for ((chunkKey, data) in entries) {
                    if (now - data.timestamp <= RebarConfig.BlockTextureConfig.OCCLUDING_CACHE_REFRESH_SHARE) continue

                    val world = Bukkit.getWorld(worldId) ?: continue
                    if (world.isChunkLoaded(chunkKey.toInt(), (chunkKey shr 32).toInt())) {
                        data.timestamp = now
                        data.occluding.cleanUp()
                        data.occluding.invalidateAll()
                        if (++refreshed >= toRefresh) break
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
            delay(500L) // TODO: Make configurable, currently every 10 ticks

            for ((uuid, tasks) in syncJobTasks) {
                val player = Bukkit.getPlayer(uuid) ?: continue
                for ((block, shouldShow) in tasks) {
                    if (shouldShow) {
                        block.showEntities(player)
                    } else {
                        block.hideEntities(player)
                    }
                }
                tasks.clear()
            }

            for ((uuid, groupTasks) in syncJobGroupTasks) {
                val player = Bukkit.getPlayer(uuid) ?: continue
                for ((block, tasks) in groupTasks) {
                    for ((id, shouldShow) in tasks) {
                        if (shouldShow) {
                            block.showEntities(player, id)
                        } else {
                            block.hideEntities(player, id)
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
    var Player.hasCustomBlockTextures: Boolean
        get() = (this.persistentDataContainer.getOrDefault(customBlockTexturesKey, PersistentDataType.BOOLEAN, RebarConfig.BlockTextureConfig.DEFAULT) || RebarConfig.BlockTextureConfig.FORCED)
        set(value) = this.persistentDataContainer.set(customBlockTexturesKey, PersistentDataType.BOOLEAN, value || RebarConfig.BlockTextureConfig.FORCED)

    @JvmStatic
    var Player.cullingPreset: CullingPreset
        get() = RebarConfig.BlockTextureConfig.CULLING_PRESETS.getOrElse(this.persistentDataContainer.getOrDefault(presetKey, PersistentDataType.STRING, RebarConfig.BlockTextureConfig.DEFAULT_CULLING_PRESET.id)) {
            RebarConfig.BlockTextureConfig.DEFAULT_CULLING_PRESET
        }
        set(value) {
            this.persistentDataContainer.set(presetKey, PersistentDataType.STRING, value.id)
            if (value.id == DISABLED_PRESET) {
                getOctree(this.world, culledBlockOctrees).forEach { block ->
                    (block as? RebarCulledBlock)?.showEntities(this)
                }
            }
        }

    @JvmSynthetic
    internal fun insert(block: RebarBlock) {
        if (!RebarConfig.BlockTextureConfig.ENABLED) return
        if (!block.disableBlockTextureEntity) {
            getOctree(block.block.world, blockTextureOctrees).insert(block)
        }
        if (block is RebarCulledBlock || block is RebarGroupCulledBlock) {
            getOctree(block.block.world, culledBlockOctrees).insert(block)
        }
    }

    @JvmSynthetic
    internal fun remove(block: RebarBlock) {
        if (!RebarConfig.BlockTextureConfig.ENABLED) return
        if (!block.disableBlockTextureEntity) {
            getOctree(block.block.world, blockTextureOctrees).remove(block)
            block.blockTextureEntity?.removeAllViewers()
        }
        if (block is RebarCulledBlock || block is RebarGroupCulledBlock) {
            getOctree(block.block.world, culledBlockOctrees).remove(block)
        }
    }

    @JvmSynthetic
    internal fun getOctree(world: World, octrees: MutableMap<UUID, Octree<RebarBlock>>): Octree<RebarBlock> {
        check(RebarConfig.BlockTextureConfig.ENABLED) { "Tried to access BlockTextureEngine octree while custom block textures are disabled" }

        val border = world.worldBorder
        return octrees.getOrPut(world.uid) {
            Octree(
                bounds = BoundingBox.of(
                    Vector(border.center.x - border.size / 2, world.minHeight.toDouble(), border.center.z - border.size / 2),
                    Vector(border.center.x + border.size / 2, world.maxHeight.toDouble(), border.center.z + border.size / 2)
                ),
                depth = 0,
                entryStrategy = { BoundingBox.of(it.block) }
            )
        }
    }

    @JvmSynthetic
    internal fun launchBlockTextureJob(player: Player) {
        val uuid = player.uniqueId
        if (!RebarConfig.BlockTextureConfig.ENABLED || jobs.containsKey(uuid)) return

        jobs[uuid] = Rebar.launch(Rebar.asyncDispatcher) {
            val visible = mutableSetOf<RebarBlock>()
            var tick = 0

            while (true) {
                val player = Bukkit.getPlayer(uuid)
                if (player == null) {
                    blockTextureOctrees.values.forEach { it.forEach { b -> b.blockTextureEntity?.removeViewer(uuid) } }
                    jobs.remove(uuid)
                    break
                }

                // When showing/hiding entities, we will always add/remove the viewer and add/remove the block from the visible set
                // because in some edge cases, the visible set and the actual viewers can get out of sync

                val world = player.world
                val occludingCache = occludingCache.getOrPut(world.uid) { mutableMapOf() }
                val syncTasks = syncJobTasks.getOrPut(uuid) { ConcurrentHashMap() }
                val syncGroupTasks = syncJobGroupTasks.getOrPut(uuid) { ConcurrentHashMap() }

                val location = player.location
                val eye = player.eyeLocation.toVector()
                val preset = player.cullingPreset
                val blockTextureOctree = getOctree(world, blockTextureOctrees)
                if (preset.id == DISABLED_PRESET) {
                    if (player.hasCustomBlockTextures) {
                        // Send all entities within view distance and hide all others
                        val radius = player.sendViewDistance * 16 / 2.0
                        val query = blockTextureOctree.query(BoundingBox.of(eye, radius, radius, radius))
                        visible.toSet().subtract(query.toSet()).forEach { it.blockTextureEntity?.removeViewer(uuid) }
                        visible.clear()

                        for (block in query) {
                            val entity = block.blockTextureEntity ?: continue
                            val distanceSquared = block.block.location.distanceSquared(location)
                            entity.addOrRefreshViewer(uuid, distanceSquared)
                        }
                        visible.addAll(query)
                    }
                    delay(preset.updateInterval.ticks)
                    continue
                }

                // Query all possibly visible blocks within cull radius, and hide all others
                val culledBlockOctree = getOctree(world, culledBlockOctrees)

                val query = culledBlockOctree.query(BoundingBox.of(eye, preset.cullRadius.toDouble(), preset.cullRadius.toDouble(), preset.cullRadius.toDouble()))
                if (player.hasCustomBlockTextures) {
                    query.addAll(blockTextureOctree.query(BoundingBox.of(eye, preset.cullRadius.toDouble(), preset.cullRadius.toDouble(), preset.cullRadius.toDouble())))
                }
                visible.toSet().subtract(query.toSet()).forEach {
                    it.blockTextureEntity?.removeViewer(uuid)
                    if (it is RebarCulledBlock && it !is RebarGroupCulledBlock) {
                        syncTasks[it] = false
                    }
                }
                visible.retainAll(query)

                val cullingGroups = mutableSetOf<RebarGroupCulledBlock.CullingGroup>()

                // First step go through all blocks in the query and determine if they should be shown or hidden
                // If a block isn't a PylonGroupCulledBlock, either immediately change its visibility or schedule it if necessary (PylonCulledBlock's)
                // If it is a PylonGroupCulledBlock, handle it in the next step
                for (block in query) {
                    val entity = block.blockTextureEntity
                    val seen = entity?.viewers?.contains(uuid) ?: (block is RebarCulledBlock && block.culledEntityIds.first().let { !player.isVisibilityInverted(it) })

                    // If we are within the always show radius, show, if we are outside cull radius, hide
                    // (our query is a cube not a sphere, so blocks in the corners can still be outside the cull radius)
                    val distanceSquared = block.block.location.distanceSquared(location)

                    if (distanceSquared <= preset.alwaysShowRadius * preset.alwaysShowRadius) {
                        entity?.addOrRefreshViewer(uuid, distanceSquared)
                        if (block is RebarCulledBlock) {
                            syncTasks[block] = true
                        }
                        if (block is RebarGroupCulledBlock) {
                            cullingGroups.addAll(block.cullingGroups)
                        }
                        visible.add(block)
                        continue
                    } else if (distanceSquared > preset.cullRadius * preset.cullRadius) {
                        entity?.removeViewer(uuid)
                        if (block is RebarCulledBlock) {
                            syncTasks[block] = false
                        }
                        if (block is RebarGroupCulledBlock) {
                            cullingGroups.addAll(block.cullingGroups)
                        }
                        visible.remove(block)
                        continue
                    }

                    // If its visible & we are on a visibleInterval tick, or if its hidden & we are on a hiddenInterval tick, do a culling check
                    if ((seen && (tick % preset.visibleInterval) == 0) || (!seen && (tick % preset.hiddenInterval) == 0)) {
                        // TODO: Later if necessary, have a 3d scan using bounding boxes rather than a line
                        // Ray traces from the players eye to the center of the block, counting occluding blocks in between
                        // if its greater than the maxOccludingCount, hide the entity, otherwise show it
                        var occluding = 0
                        val end = Vector(block.block.x + 0.5, block.block.y + 0.5, block.block.z + 0.5)
                        val totalDistance = eye.distanceSquared(end)
                        val current = eye.clone()
                        val direction = end.clone().subtract(eye).normalize()
                        while (current.distanceSquared(eye) < totalDistance) {
                            current.add(direction)
                            if (current.distanceSquared(eye) > totalDistance) {
                                current.copy(end)
                            }

                            val x = current.blockX
                            val y = current.blockY
                            val z = current.blockZ

                            val chunkPos = Chunk.getChunkKey(x shr 4, z shr 4)
                            val occludes = occludingCache.getOrPut(chunkPos) { ChunkData() }.isOccluding(world, x, y, z)
                            if (occludes && ++occluding > preset.maxOccludingCount) {
                                break
                            }
                        }

                        val shouldSee = occluding <= preset.maxOccludingCount
                        if (shouldSee) {
                            entity?.addOrRefreshViewer(uuid, distanceSquared)
                            if (block is RebarCulledBlock) {
                                syncTasks[block] = true
                            }
                            if (block is RebarGroupCulledBlock) {
                                cullingGroups.addAll(block.cullingGroups)
                            }
                            visible.add(block)
                        } else {
                            entity?.removeViewer(uuid)
                            if (block is RebarCulledBlock) {
                                syncTasks[block] = false
                            }
                            if (block is RebarGroupCulledBlock) {
                                cullingGroups.addAll(block.cullingGroups)
                            }
                            visible.remove(block)
                        }
                    }
                }

                // Second step, handle group culled blocks
                // If any one member of the group is visible, all members are visible
                // This only affects the PylonCulledBlock aspect, block texture entities are never group culled
                for (group in cullingGroups) {
                    var anyVisible = false
                    for (block in group.blocks) {
                        if (visible.contains(block as RebarBlock)) {
                            anyVisible = true
                            break
                        }
                    }

                    val first = group.blocks.firstOrNull() ?: continue
                    val groupTasks = syncGroupTasks.getOrPut(first) { ConcurrentHashMap() }
                    groupTasks[group.id] = anyVisible
                }

                delay(preset.updateInterval.ticks)
                tick++
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun onWorldLoad(event: WorldLoadEvent) {
        occludingCache[event.world.uid] = mutableMapOf()
        getOctree(event.world, blockTextureOctrees)
        getOctree(event.world, culledBlockOctrees)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun onPlayerJoin(event: PlayerJoinEvent) {
        launchBlockTextureJob(event.player)
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

    private data class ChunkData(
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
