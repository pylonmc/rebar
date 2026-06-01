package io.github.pylonmc.rebar.item.interfaces

import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import io.github.pylonmc.rebar.item.research.Research.Companion.canUse
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageAbortEvent
import org.bukkit.event.block.BlockDamageEvent
import org.jetbrains.annotations.ApiStatus

interface BlockBreakRebarItemHandler {
    /**
     * Called when the item is used to damage a block.
     */
    fun onDamageBlock(event: BlockDamageEvent, priority: EventPriority) {}

    /**
     * Called when the item was being used to break a block by a player and they stop
     */
    fun onAbortBlockDamage(event: BlockDamageAbortEvent, priority: EventPriority) {}

    /**
     * Called when the item is used to break a block.
     */
    fun onBreakBlock(event: BlockBreakEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onDamageBlock(event: BlockDamageEvent, priority: EventPriority) {
            val rebarItem = RebarItem.fromStack(event.itemInHand, BlockBreakRebarItemHandler::class.java)
            if (rebarItem !is RebarItem) return
            if (!event.player.canUse(rebarItem, false)) {
                event.isCancelled = true
                return
            }

            try {
                MultiHandlers.handleEvent(rebarItem, "onDamageBlock", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }

        @UniversalHandler
        private fun onAbortBlockDamage(event: BlockDamageAbortEvent, priority: EventPriority) {
            val rebarItem = RebarItem.fromStack(event.itemInHand, BlockBreakRebarItemHandler::class.java)
            if (rebarItem !is RebarItem) return
            if (!event.player.canUse(rebarItem, false)) {
                return
            }

            try {
                MultiHandlers.handleEvent(rebarItem, "onAbortBlockDamage", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }

        @UniversalHandler
        private fun onBreakBlock(event: BlockBreakEvent, priority: EventPriority) {
            val rebarItemMainHand = RebarItem.fromStack(event.player.inventory.itemInMainHand, BlockBreakRebarItemHandler::class.java)
            if (rebarItemMainHand !is RebarItem) return
            if (!event.player.canUse(rebarItemMainHand, false)) {
                event.isCancelled = true
                return
            }

            try {
                MultiHandlers.handleEvent(rebarItemMainHand, "onBreakBlock", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItemMainHand)
            }
        }
    }
}