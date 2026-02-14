package io.github.pylonmc.rebar.culling

import com.github.shynixn.mccoroutine.bukkit.ticks
import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.block.base.RebarCulledBlock
import io.github.pylonmc.rebar.block.base.RebarGroupCulledBlock
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.culling.BlockCullingEngine.ChunkData
import io.github.pylonmc.rebar.culling.BlockCullingEngine.hasBlockCulling
import io.github.pylonmc.rebar.culling.BlockCullingEngine.blockTextureOctrees
import io.github.pylonmc.rebar.culling.BlockCullingEngine.culledBlockOctrees
import io.github.pylonmc.rebar.culling.BlockCullingEngine.getOctree
import io.github.pylonmc.rebar.culling.BlockCullingEngine.occludingCache
import io.github.pylonmc.rebar.culling.BlockCullingEngine.playerCullingConfig
import io.github.pylonmc.rebar.culling.BlockCullingEngine.syncJobGroupTasks
import io.github.pylonmc.rebar.culling.BlockCullingEngine.syncJobTasks
import io.github.pylonmc.rebar.resourcepack.block.BlockTextureEngine.hasCustomBlockTextures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.addAll
import kotlin.collections.forEach
import kotlin.collections.set

class PlayerCullingJob(
    val playerId: UUID,
    val visible: MutableSet<RebarBlock> = mutableSetOf(),
    var tick: Int = 0
) {
    var running = true

    val job: suspend CoroutineScope.() -> Unit = {
        while (running) {
            run()
        }
    }

    suspend fun run() {
        val player = Bukkit.getPlayer(playerId)
        if (player == null) {
            BlockCullingEngine.stopCullingJob(playerId)
            running = false
            return
        }

        // When showing/hiding entities, we will always add/remove the viewer and add/remove the block from the visible set
        // because in some edge cases, the visible set and the actual viewers can get out of sync

        val world = player.world
        val feet = player.location.toVector()
        val eye = player.eyeLocation.toVector()
        val cullBox = player.cullingBoundingBox
        val blockTextureOctree = getOctree(world, blockTextureOctrees)
        if (!player.hasBlockCulling) {
            if (player.hasCustomBlockTextures) {
                // Send all entities within view distance and hide all others
                val query = blockTextureOctree.query(cullBox)
                visible.toSet().subtract(query.toSet()).forEach { it.blockTextureEntity?.removeViewer(playerId) }
                visible.clear()

                for (block in query) {
                    val entity = block.blockTextureEntity ?: continue
                    val distanceSquared = block.block.distanceSquared(feet)
                    entity.addOrRefreshViewer(playerId, distanceSquared)
                }
                visible.addAll(query)
            }
            delay(RebarConfig.CullingEngineConfig.DISABLED_UPDATE_INTERVAL.ticks)
            return
        }

        val occludingCache = occludingCache.getOrPut(world.uid) { mutableMapOf() }
        val syncTasks = syncJobTasks.getOrPut(playerId) { ConcurrentHashMap() }
        val syncGroupTasks = syncJobGroupTasks.getOrPut(playerId) { ConcurrentHashMap() }

        val config = player.playerCullingConfig

        // Query all possibly visible blocks within cull radius, and hide all others
        val culledBlockOctree = getOctree(world, culledBlockOctrees)

        val query = culledBlockOctree.query(cullBox)
        if (player.hasCustomBlockTextures) {
            query.addAll(blockTextureOctree.query(cullBox))
        }
        visible.toSet().subtract(query.toSet()).forEach {
            it.blockTextureEntity?.removeViewer(playerId)
            if (it is RebarCulledBlock && it !is RebarGroupCulledBlock) {
                syncTasks[it] = false
            }
        }
        visible.retainAll(query)

        val cullingGroups = mutableSetOf<RebarGroupCulledBlock.CullingGroup>()

        fun makeBlockVisible(block: RebarBlock, distanceSquared: Double) {
            block.blockTextureEntity?.addOrRefreshViewer(playerId, distanceSquared)
            if (block is RebarGroupCulledBlock) {
                cullingGroups.addAll(block.cullingGroups)
            } else if (block is RebarCulledBlock) {
                if (block.isCulledAsync) {
                    block.onVisible(player)
                } else {
                    syncTasks[block] = true
                }
            }
            visible.add(block)
        }

        fun makeBlockCulled(block: RebarBlock) {
            block.blockTextureEntity?.removeViewer(playerId)
            if (block is RebarGroupCulledBlock) {
                cullingGroups.addAll(block.cullingGroups)
            } else if (block is RebarCulledBlock) {
                if (block.isCulledAsync) {
                    block.onCulled(player)
                } else {
                    syncTasks[block] = false
                }
            }
            visible.remove(block)
        }

        // First step go through all blocks in the query and determine if they should be shown or hidden
        // If a block isn't a PylonGroupCulledBlock, either immediately change its visibility or schedule it if necessary (PylonCulledBlock's)
        // If it is a PylonGroupCulledBlock, handle it in the next step
        for (block in query) {
            val entity = block.blockTextureEntity
            val seen = when (block) {
                is RebarCulledBlock -> block.isVisible(player)
                else -> entity?.hasViewer(playerId) ?: false
            }

            // If we are within the always show radius, show, if we are outside cull radius, hide
            // (our query is a cube not a sphere, so blocks in the corners can still be outside the cull radius)
            val distanceSquared = block.block.distanceSquared(feet)
            if (distanceSquared <= config.alwaysShowRadius * config.alwaysShowRadius) {
                makeBlockVisible(block, distanceSquared)
                continue
            } else if (distanceSquared > config.cullRadius * config.cullRadius) {
                makeBlockCulled(block)
                continue
            }

            // If its visible & we are on a visibleInterval tick, or if its hidden & we are on a hiddenInterval tick, do a culling check
            if ((seen && (tick % config.visibleInterval) == 0) || (!seen && (tick % config.hiddenInterval) == 0)) {
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
                    if (occludes && ++occluding > config.maxOccludingCount) {
                        break
                    }
                }

                val shouldSee = occluding <= config.maxOccludingCount
                if (shouldSee) {
                    makeBlockVisible(block, distanceSquared)
                } else {
                    makeBlockCulled(block)
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
            if (first.isCulledAsync) {
                if (anyVisible) {
                    first.onGroupVisible(player, group)
                } else {
                    first.onGroupCulled(player, group)
                }
            } else {
                val groupTasks = syncGroupTasks.getOrPut(first) { ConcurrentHashMap() }
                groupTasks[group] = anyVisible
            }
        }

        delay(config.updateInterval.ticks)
        tick++
    }

    companion object {
        internal val Player.cullingBoundingBox: BoundingBox
            get() = run {
                val radius = when(hasBlockCulling) {
                    true -> playerCullingConfig.cullRadius.toDouble()
                    false -> sendViewDistance * 16 / 2.0
                }
                return BoundingBox.of(eyeLocation, radius, radius, radius)
            }

        private fun Block.distanceSquared(point: Vector): Double {
            val x = this.x + 0.5 - point.x
            val y = this.y + 0.5 - point.y
            val z = this.z + 0.5 - point.z
            return x * x + y * y + z * z
        }
    }
}