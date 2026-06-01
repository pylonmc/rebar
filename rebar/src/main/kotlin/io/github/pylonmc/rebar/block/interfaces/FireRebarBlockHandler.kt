package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.block.context.BlockCreateContext
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockSpreadEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack
import org.jetbrains.annotations.ApiStatus

@Suppress("unused")
interface FireRebarBlockHandler {
    fun onFireDamageEntity(event: EntityDamageEvent, priority: EventPriority) {}

    /**
     * Called when this fire block ignites block
     * Spawns copies of this fire block itself after calling, if the event is not cancelled
     */
    fun onFireIgniteBlock(event: BlockIgniteEvent, priority: EventPriority) {}

    /**
     * Spawns copies of this fire block itself after calling, if the event is not cancelled
     */
    fun onFireSpread(event: BlockSpreadEvent, priority: EventPriority) {}

    /**
     * Called when this fire block burns another block
     * @return true if the fire should attempt to spread to the burned block, false otherwise
     *
     * Note: This is always processed at [EventPriority.HIGHEST] and cannot be used with [MultiHandler]
     */
    fun onFireBurnBlock(event: BlockBurnEvent): Boolean = true

    data class FireSpread(
        val sourceBlock: Block,
        override val block: Block
    ): BlockCreateContext {
        override val player: Player? = null
        override val facing: BlockFace = BlockFace.NORTH
        override val facingVertical: BlockFace = BlockFace.NORTH
        override val item: ItemStack? = null
        override val shouldSetType: Boolean = false
    }

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onFireDamageEntity(event: EntityDamageEvent, priority: EventPriority) {
            if (event.cause != EntityDamageEvent.DamageCause.FIRE) return

            val rebarBlock = BlockStorage.get(event.damageSource.sourceLocation)
            if (rebarBlock !is FireRebarBlockHandler) {
                return
            }

            try {
                MultiHandlers.handleEvent(rebarBlock, "onFireDamageEntity", event, priority)
            } catch (e: Exception) {
                BlockListener.logEventHandleErr(event, e, rebarBlock)
            }
        }

        @UniversalHandler
        private fun onFireIgniteBlock(event: BlockIgniteEvent, priority: EventPriority) {
            if (event.ignitingBlock == null || event.cause != BlockIgniteEvent.IgniteCause.SPREAD) {
                return
            }

            val rebarBlock = BlockStorage.get(event.ignitingBlock!!)
            if (rebarBlock !is FireRebarBlockHandler) return

            try {
                MultiHandlers.handleEvent(rebarBlock, "onFireIgniteBlock", event, priority)
                if (priority == EventPriority.MONITOR && !event.isCancelled) {
                    trySpreadFire(rebarBlock, event.block)
                }
            } catch (e: Exception) {
                BlockListener.logEventHandleErr(event, e, rebarBlock)
            }
        }

        @UniversalHandler
        private fun onFireSpread(event: BlockSpreadEvent, priority: EventPriority) {
            if (!Tag.FIRE.isTagged(event.block.type)) return

            val rebarBlock = BlockStorage.get(event.source)
            if (rebarBlock !is FireRebarBlockHandler) return

            try {
                MultiHandlers.handleEvent(rebarBlock, "onFireSpread", event, priority)
                if (priority == EventPriority.MONITOR && !event.isCancelled) {
                    trySpreadFire(rebarBlock, event.block)
                }
            } catch (e: Exception) {
                BlockListener.logEventHandleErr(event, e, rebarBlock)
            }
        }

        /**
         * This is a hack to make fire spread work. After calling BlockBurnEvent,
         * the fire still has a chance to spawn a new fire at the block
         * without calling events again.
         *
         * TODO: paper pr for proper event calls for the fire spread that occurs after this event
         */
        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        private fun onFireBurnBlock(event: BlockBurnEvent) {
            val rebarBlock = BlockStorage.get(event.ignitingBlock)
            if (rebarBlock !is FireRebarBlockHandler) return
            val canSpread = rebarBlock.onFireBurnBlock(event)
            val sourceType = rebarBlock.block.type

            Bukkit.getScheduler().runTask(Rebar) { _ ->
                if (event.block.type == sourceType) {
                    if (canSpread) {
                        trySpreadFire(rebarBlock, event.block)
                    } else {
                        event.block.type = Material.AIR
                    }
                }
            }
        }

        private fun <T> trySpreadFire(sourceFire: T, spreadBlock: Block) where T : FireRebarBlockHandler, T : RebarBlock {
            if (BlockStorage.isRebarBlock(spreadBlock)) {
                return
            }

            BlockStorage.placeBlock(
                spreadBlock,
                sourceFire.key,
                FireSpread(sourceFire.block, spreadBlock)
            )
        }
    }
}