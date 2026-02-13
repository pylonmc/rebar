package io.github.pylonmc.rebar.resourcepack.block

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.entity.packet.BlockTextureEntity
import io.github.pylonmc.rebar.event.RebarBlockBreakEvent
import io.github.pylonmc.rebar.util.Octree
import io.github.pylonmc.rebar.util.position.BlockPosition
import io.github.pylonmc.rebar.util.position.ChunkPosition
import io.github.pylonmc.rebar.util.rebarKey
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import java.time.Duration
import java.util.UUID
import kotlin.collections.Map.Entry
import kotlin.math.ceil

object BlockTextureEngine : Listener {
    const val DISABLED_PRESET = "disabled"

    val customBlockTexturesKey = rebarKey("custom_block_textures")
    val presetKey = rebarKey("culling_preset")

    private val occludingCache = mutableMapOf<ChunkPosition, ChunkData>()

    private val octrees = mutableMapOf<UUID, Octree<RebarBlock>>()
    private val jobs = mutableMapOf<UUID, Job>()

    /**
     * Periodically updates a share of the occluding cache, to ensure it stays up to date with changes in the world.
     * Every [RebarConfig.BlockTextureConfig.OCCLUDING_CACHE_REFRESH_INTERVAL] ticks, it will refresh [RebarConfig.BlockTextureConfig.OCCLUDING_CACHE_REFRESH_SHARE]
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
            var refreshed = 0
            var toRefresh = ceil(occludingCache.size * RebarConfig.BlockTextureConfig.OCCLUDING_CACHE_REFRESH_SHARE)
            var entries = mutableListOf<Entry<ChunkPosition, ChunkData>>()
            entries.addAll(occludingCache.entries)
            entries.sortBy { it.value.timestamp }

            for ((pos, data) in entries) {
                if (now - data.timestamp <= RebarConfig.BlockTextureConfig.OCCLUDING_CACHE_REFRESH_INTERVAL) continue

                val world = pos.world ?: continue
                if (world.isChunkLoaded(pos.x, pos.z)) {
                    data.timestamp = now
                    data.occluding.cleanUp()
                    for (position in data.occluding.asMap().keys.toSet()) {
                        data.occluding.put(position, position.block.blockData.isOccluding)
                    }

                    if (++refreshed >= toRefresh) break
                } else {
                    occludingCache.remove(pos)
                }
            }
        }
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
        set(value) = this.persistentDataContainer.set(presetKey, PersistentDataType.STRING, value.id)

    @JvmSynthetic
    internal fun insert(block: RebarBlock) {
        if (!RebarConfig.BlockTextureConfig.ENABLED || block.disableBlockTextureEntity) return
        getOctree(block.block.world).insert(block)
    }

    @JvmSynthetic
    internal fun remove(block: RebarBlock) {
        if (!RebarConfig.BlockTextureConfig.ENABLED || block.disableBlockTextureEntity) return
        getOctree(block.block.world).remove(block)

        if ((block::blockTextureEntity as Lazy<*>).isInitialized()) {
            block.blockTextureEntity?.let {
                for (viewer in it.viewers.toSet()) {
                    it.removeViewer(viewer)
                }
            }
        }
    }

    @JvmSynthetic
    internal fun getOctree(world: World): Octree<RebarBlock> {
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
        if (!RebarConfig.BlockTextureConfig.ENABLED || !player.hasCustomBlockTextures || jobs.containsKey(uuid)) return

        jobs[uuid] = Rebar.launch(Rebar.asyncDispatcher) {
            val visible = mutableSetOf<RebarBlock>()
            var tick = 0

            while (true) {
                val player = Bukkit.getPlayer(uuid)
                if (player == null || !player.hasCustomBlockTextures) {
                    octrees.values.forEach { it.forEach { b -> b.blockTextureEntity?.removeViewer(uuid) } }
                    jobs.remove(uuid)
                    break
                }

                // When showing/hiding entities, we will always add/remove the viewer and add/remove the block from the visible set
                // because in some edge cases, the visible set and the actual viewers can get out of sync

                val world = player.world
                val eye = player.eyeLocation.toVector()
                val preset = player.cullingPreset
                val octree = getOctree(world)
                if (preset.id == DISABLED_PRESET) {
                    // Send all block entities within view distance and hide all others
                    val radius = player.sendViewDistance * 16 / 2.0
                    val query = octree.query(BoundingBox.of(eye, radius, radius, radius))
                    visible.toSet().subtract(query.toSet()).forEach { it.blockTextureEntity?.removeViewer(uuid) }

                    for (block in query) {
                        val entity = block.blockTextureEntity ?: continue
                        val distanceSquared = block.block.location.distanceSquared(player.location)
                        entity.addOrRefreshViewer(uuid, distanceSquared)
                        visible.add(block)
                    }
                    delay(preset.updateInterval.ticks)
                    continue
                }

                // Query all possibly visible blocks within cull radius, and hide all others
                val query = octree.query(BoundingBox.of(eye, preset.cullRadius.toDouble(), preset.cullRadius.toDouble(), preset.cullRadius.toDouble()))
                visible.toSet().subtract(query.toSet()).forEach { it.blockTextureEntity?.removeViewer(uuid) }

                for (block in query) {
                    val entity = block.blockTextureEntity ?: continue

                    // If we are within the always show radius, show, if we are outside cull radius, hide
                    // (our query is a cube not a sphere, so blocks in the corners can still be outside the cull radius)
                    val seen = entity.hasViewer(uuid)
                    val distanceSquared = block.block.location.distanceSquared(player.location)
                    if (distanceSquared <= preset.alwaysShowRadius * preset.alwaysShowRadius) {
                        entity.addOrRefreshViewer(uuid, distanceSquared)
                        visible.add(block)
                        continue
                    } else if (distanceSquared > preset.cullRadius * preset.cullRadius) {
                        entity.removeViewer(uuid)
                        visible.remove(block)
                        continue
                    }

                    // If its visible & we are on a visibleInterval tick, or if its hidden & we are on a hiddenInterval tick, do a culling check
                    if ((seen && (tick % preset.visibleInterval) == 0) || (!seen && (tick % preset.hiddenInterval) == 0)) {
                        // TODO: Later if necessary, have a 3d scan using bounding boxes rather than a line
                        // Ray traces from the players eye to the center of the block, counting occluding blocks in between
                        // if its greater than the maxOccludingCount, hide the entity, otherwise show it
                        var occluding = 0
                        val end = block.block.location.toCenterLocation().toVector()
                        val totalDistance = eye.distanceSquared(end)
                        val current = eye.clone()
                        val direction = end.clone().subtract(eye).normalize()
                        while (current.distanceSquared(eye) < totalDistance) {
                            current.add(direction)
                            if (current.distanceSquared(eye) > totalDistance) {
                                current.copy(end)
                            }

                            val position = BlockPosition(world, current)
                            val chunkPos = position.chunk
                            val occludes = occludingCache[chunkPos]?.isOccluding(position) ?: current.toLocation(world).block.blockData.isOccluding
                            if (occludes && ++occluding > preset.maxOccludingCount) {
                                break
                            }
                        }

                        val shouldSee = occluding <= preset.maxOccludingCount
                        if (shouldSee) {
                            entity.addOrRefreshViewer(uuid, distanceSquared)
                            visible.add(block)
                        } else {
                            entity.removeViewer(uuid)
                            visible.remove(block)
                        }
                    }
                }

                delay(preset.updateInterval.ticks)
                tick++
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun onPlayerJoin(event: PlayerJoinEvent) {
        launchBlockTextureJob(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun onChunkLoad(event: ChunkLoadEvent) {
        occludingCache[ChunkPosition(event.chunk)] = ChunkData(System.currentTimeMillis())
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onBlockPlace(event: BlockPlaceEvent) {
        occludingCache[ChunkPosition(event.block)]?.occluding?.put(BlockPosition(event.block), event.block.blockData.isOccluding)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onBlockBreak(event: BlockBreakEvent) {
        occludingCache[ChunkPosition(event.block)]?.occluding?.put(BlockPosition(event.block), false)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun onRebarBlockBreak(event: RebarBlockBreakEvent) {
        // TODO: Delete this when Rebar no longer cancels BlockBreakEvents for RebarBlocks
        occludingCache[ChunkPosition(event.block)]?.occluding?.put(BlockPosition(event.block), false)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun onChunkUnload(event: ChunkUnloadEvent) {
        occludingCache.remove(ChunkPosition(event.chunk))
    }

    private data class ChunkData(
        var timestamp: Long,
        val occluding: LoadingCache<BlockPosition, Boolean> = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(1))
            .build { pos -> pos.block.blockData.isOccluding },
    ) {
        fun isOccluding(position: BlockPosition): Boolean {
            return occluding.get(position)
        }
    }
}
