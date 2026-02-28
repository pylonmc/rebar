package io.github.pylonmc.rebar.block

import com.destroystokyo.paper.event.block.BlockDestroyEvent
import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.block.base.RebarFallingBlock
import io.github.pylonmc.rebar.block.base.RebarTickingBlock
import io.github.pylonmc.rebar.block.context.BlockBreakContext
import io.github.pylonmc.rebar.block.context.BlockCreateContext
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.research.Research.Companion.canUse
import io.github.pylonmc.rebar.util.isFakeEvent
import io.github.pylonmc.rebar.util.position.position
import io.papermc.paper.event.block.BlockBreakBlockEvent
import org.bukkit.ExplosionResult
import org.bukkit.Material
import org.bukkit.entity.FallingBlock
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityDropItemEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.EntityRemoveEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.world.StructureGrowEvent
import java.util.UUID
import java.util.WeakHashMap


/**
 * This listener listens for various events that would indicate a Rebar block either
 * being placed, removed, or moved
 *
 * It also handles components of multiblocks being placed, removed, or moved (this
 * includes vanilla blocks)
 */
@Suppress("UnstableApiUsage")
internal object BlockListener : MultiListener {
    private val blockErrMap: MutableMap<RebarBlock, Int> = WeakHashMap()
    
    @MultiHandler(priorities = [ EventPriority.LOWEST, EventPriority.MONITOR ], ignoreCancelled = true)
    private fun blockPlace(event: BlockPlaceEvent, priority: EventPriority) {
        val item = event.itemInHand
        val player = event.player

        if (!item.type.isBlock) {
            return
        }

        val rebarItem = RebarItem.fromStack(item) ?: return
        if (!event.player.canUse(rebarItem, true)) {
            event.isCancelled = true
            return
        }

        if (isFakeEvent(event)) {
            // Fake events are for checking permissions, no need to do anything but check permission.
            if (rebarItem.schema.rebarBlockKey == null
                || BlockStorage.isRebarBlock(event.block)
            ) {
                event.isCancelled = true
                return
            }
        }

        val context = BlockCreateContext.PlayerPlace(player, item, event)
        if (priority == EventPriority.LOWEST && !rebarItem.prePlace(context)) {
            event.isCancelled = true
        } else if (priority == EventPriority.MONITOR) {
            rebarItem.place(context)
        }
    }

    private val fallMap = HashMap<UUID, Pair<RebarFallingBlock, RebarFallingBlock.RebarFallingBlockEntity>>();

    @EventHandler(ignoreCancelled = true)
    private fun entityBlockChange(event: EntityChangeBlockEvent) {
        val entity = event.entity

        if (entity !is FallingBlock) return

        val block = event.block
        if (!entity.isInWorld) {
            val rebarBlock = BlockStorage.get(block) ?: return
            val rebarFallingBlock = rebarBlock as? RebarFallingBlock
            if (rebarFallingBlock == null) {
                event.isCancelled = true
                return
            }

            val blockPdc = RebarBlock.serialize(rebarBlock, block.chunk.persistentDataContainer.adapterContext)
            val fallingEntity = RebarFallingBlock.RebarFallingBlockEntity(rebarBlock.schema, blockPdc, block.position, entity)
            rebarFallingBlock.onFallStart(event, fallingEntity)
            if (!event.isCancelled) {
                BlockStorage.deleteBlock(block.position)
                EntityStorage.add(fallingEntity)
                // save this here as the entity storage is going to nuke it if the item drops
                fallMap[entity.uniqueId] = Pair(rebarFallingBlock, fallingEntity)
            }
        } else {
            val rebarEntity = EntityStorage.get(entity) as? RebarFallingBlock.RebarFallingBlockEntity ?: return
            val rebarBlock = BlockStorage.loadBlock(block.position, rebarEntity.blockSchema, rebarEntity.blockData) as RebarFallingBlock

            rebarBlock.onFallStop(event, rebarEntity)
        }
    }

    @EventHandler
    private fun entityDespawn(event: EntityRemoveEvent) {
        // DESPAWN = Fell and created block ; OUT_OF_WORLD = Fell and dropped item
        if (event.cause != EntityRemoveEvent.Cause.DESPAWN) return
        val entity = event.entity
        if (entity !is FallingBlock) return
        fallMap.remove(entity.uniqueId)
    }

    @EventHandler(ignoreCancelled = true)
    private fun fallingBlockDrop(event: EntityDropItemEvent) {
        val entity = event.entity

        if (entity !is FallingBlock) return

        val (rebarFallingBlock, rebarFallingEntity) = fallMap[entity.uniqueId] ?: return
        fallMap.remove(entity.uniqueId)

        val relativeItem = rebarFallingBlock.onItemDrop(event, rebarFallingEntity)
        if (event.isCancelled) return
        if (relativeItem == null) {
            event.isCancelled = true
            return
        }

        event.itemDrop.itemStack = relativeItem
    }

    @MultiHandler(priorities = [ EventPriority.LOWEST, EventPriority.MONITOR ], ignoreCancelled = true)
    private fun blockRemove(event: BlockBreakEvent, priority: EventPriority) {
        val block = BlockStorage.get(event.block) ?: return
        val context = BlockBreakContext.PlayerBreak(event);
        if (priority == EventPriority.LOWEST) {
            val schema = block.defaultItem
            if (schema != null && !event.player.canUse(schema, true)) {
                return
            }

            if (!BlockStorage.preBreakBlock(block, context)) {
                event.isCancelled = true
                return
            }

            event.isDropItems = false
            event.expToDrop = 0
        } else {
            BlockStorage.removeBlock(block, event.block.position, context)
        }
    }

    @MultiHandler(priorities = [ EventPriority.LOWEST, EventPriority.MONITOR ], ignoreCancelled = true)
    private fun blockBurn(event: BlockBurnEvent, priority: EventPriority) {
        val block = BlockStorage.get(event.block) ?: return
        val context = BlockBreakContext.Burned(event);
        if (priority == EventPriority.LOWEST) {
            if (!BlockStorage.preBreakBlock(block, context)) {
                event.isCancelled = true
                return
            }
        } else {
            BlockStorage.removeBlock(block, event.block.position, context)
        }
    }

    @MultiHandler(priorities = [ EventPriority.LOWEST, EventPriority.MONITOR ], ignoreCancelled = true)
    private fun blockRemove(event: BlockExplodeEvent, priority: EventPriority) {
        if (event.explosionResult == ExplosionResult.TRIGGER_BLOCK || event.explosionResult == ExplosionResult.KEEP) {
            return
        }

        val explodingBlock = BlockStorage.get(event.block)
        if (explodingBlock != null) {
            val context = BlockBreakContext.BlockExplosionOrigin(event)
            if (priority == EventPriority.LOWEST) {
                if (!BlockStorage.preBreakBlock(explodingBlock, context)) {
                    event.isCancelled = true
                    return
                }
            } else {
                BlockStorage.removeBlock(explodingBlock, event.block.position, context)
            }
        }

        val it = event.blockList().iterator()
        while (it.hasNext()) {
            val block = it.next()
            val rebarBlock = BlockStorage.get(block) ?: continue
            val context = BlockBreakContext.BlockExploded(block, event)
            if (priority == EventPriority.LOWEST) {
                if (!BlockStorage.preBreakBlock(rebarBlock, context)) {
                    it.remove()
                }
            } else {
                BlockStorage.removeBlock(rebarBlock, block.position, context)
            }
        }
    }

    @MultiHandler(priorities = [ EventPriority.LOWEST, EventPriority.MONITOR ], ignoreCancelled = true)
    private fun blockRemove(event: EntityExplodeEvent, priority: EventPriority) {
        if (event.explosionResult == ExplosionResult.TRIGGER_BLOCK || event.explosionResult == ExplosionResult.KEEP) {
            return
        }

        val it = event.blockList().iterator()
        while (it.hasNext()) {
            val block = it.next()
            val rebarBlock = BlockStorage.get(block) ?: continue
            val context = BlockBreakContext.EntityExploded(block, event)
            if (priority == EventPriority.LOWEST) {
                if (!BlockStorage.preBreakBlock(rebarBlock, context)) {
                    it.remove()
                }
            } else {
                BlockStorage.removeBlock(rebarBlock, block.position, context)
            }
        }
    }

    @MultiHandler(priorities = [ EventPriority.MONITOR ])
    private fun blockRemove(event: BlockBreakBlockEvent, @Suppress("unused") priority: EventPriority) {
        val block = BlockStorage.get(event.block) ?: return
        val context = BlockBreakContext.BlockBreak(event)
        BlockStorage.removeBlock(block, event.block.position, context)
    }

    // Event added by paper, not really documented when it's called so two separate handlers might
    // fire for some block breaks but this shouldn't be an issue
    // Primarily added to handle sensitive blocks
    @MultiHandler(priorities = [ EventPriority.LOWEST, EventPriority.MONITOR ], ignoreCancelled = true)
    private fun blockRemove(event: BlockDestroyEvent, priority: EventPriority) {
        val block = BlockStorage.get(event.block) ?: return
        val context = BlockBreakContext.Destroyed(event);
        if (priority == EventPriority.LOWEST) {
            if (!BlockStorage.preBreakBlock(block, context)) {
                event.isCancelled = true
                return
            }
            event.setWillDrop(false)
        } else {
            BlockStorage.removeBlock(block, event.block.position, context)
        }
    }

    @MultiHandler(priorities = [ EventPriority.LOWEST, EventPriority.MONITOR ], ignoreCancelled = true)
    private fun blockRemove(event: BlockFadeEvent, priority: EventPriority) {
        val block = BlockStorage.get(event.block) ?: return
        val context = BlockBreakContext.Faded(event);
        if (priority == EventPriority.LOWEST) {
            if (!BlockStorage.preBreakBlock(block, context)) {
                event.isCancelled = true
                return
            }
        } else {
            BlockStorage.removeBlock(block, event.block.position, context)
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun disallowForming(event: BlockFormEvent) {
        if (BlockStorage.isRebarBlock(event.block)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun disallowFromTo(event: BlockFromToEvent) {
        if (BlockStorage.isRebarBlock(event.toBlock)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun disallowMovementByPistons(event: BlockPistonExtendEvent) {
        for (block in event.blocks) {
            if (BlockStorage.isRebarBlock(block)) {
                event.isCancelled = true
                return
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun disallowMovementByPistons(event: BlockPistonRetractEvent) {
        for (block in event.blocks) {
            if (BlockStorage.isRebarBlock(block)) {
                event.isCancelled = true
                return
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun disallowStructureGrow(event: StructureGrowEvent) {
        for (state in event.blocks) {
            if (BlockStorage.isRebarBlock(state.block)) {
                event.isCancelled = true
                return
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun preventReplacingStructureVoids(event: BlockPlaceEvent) {
        val rebarBlock = BlockStorage.get(event.block)
        if (rebarBlock != null && rebarBlock.schema.material == Material.STRUCTURE_VOID) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun onFluidPlace(event: PlayerBucketEmptyEvent) {
        val rebarBlock = BlockStorage.get(event.block)
        if (rebarBlock != null && rebarBlock.schema.material == Material.STRUCTURE_VOID) {
            event.isCancelled = true
        }
    }

    @JvmSynthetic
    internal fun BlockListener.logEventHandleErr(event: Event?, e: Exception, block: RebarBlock) {
        if (event != null) {
            Rebar.logger.severe("Error when handling block(${block.key}, ${block.block.location}) event handler ${event.javaClass.simpleName}: ${e.localizedMessage}")
        } else {
            Rebar.logger.severe("Error when handling block(${block.key}, ${block.block.location}) ticking: ${e.localizedMessage}")
        }
        e.printStackTrace()
        blockErrMap[block] = blockErrMap[block]?.plus(1) ?: 1
        if (blockErrMap[block]!! > RebarConfig.ALLOWED_BLOCK_ERRORS) {
            BlockStorage.makePhantom(block)
            if (block is RebarTickingBlock) {
                RebarTickingBlock.stopTicking(block)
            }
        }
    }
}