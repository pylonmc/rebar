package io.github.pylonmc.rebar.item.base

import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import io.github.pylonmc.rebar.item.research.Research.Companion.canUse
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageEvent

interface RebarTool {
    /**
     * Called when the item is used to damage a block.
     */
    fun onUsedToDamageBlock(event: BlockDamageEvent, priority: EventPriority) {}

    /**
     * Called when the item is used to break a block.
     */
    fun onUsedToBreakBlock(event: BlockBreakEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onUsedToDamageBlock(event: BlockDamageEvent, priority: EventPriority) {
            val rebarItem = RebarItem.fromStack(event.itemInHand)
            if (rebarItem !is RebarTool) return
            if (!event.player.canUse(rebarItem, false)) {
                event.isCancelled = true
                return
            }

            try {
                MultiHandlers.handleEvent(rebarItem, "onUsedToDamageBlock", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }

        @UniversalHandler
        private fun onUsedToBreakBlock(event: BlockBreakEvent, priority: EventPriority) {
            val rebarItemMainHand = RebarItem.fromStack(event.player.inventory.itemInMainHand)
            if (rebarItemMainHand is RebarTool) {
                if (!event.player.canUse(rebarItemMainHand, false)) {
                    event.isCancelled = true
                    return
                }

                try {
                    MultiHandlers.handleEvent(rebarItemMainHand, "onUsedToBreakBlock", event, priority)
                } catch (e: Exception) {
                    RebarItemListener.logEventHandleErr(event, e, rebarItemMainHand)
                }
            }

            val rebarItemOffHand = RebarItem.fromStack(event.player.inventory.itemInOffHand)
            if (rebarItemOffHand is RebarTool) {
                if (!event.player.canUse(rebarItemOffHand, false)) {
                    event.isCancelled = true
                    return
                }

                try {
                    MultiHandlers.handleEvent(rebarItemOffHand, "onUsedToBreakBlock", event, priority)
                } catch (e: Exception) {
                    RebarItemListener.logEventHandleErr(event, e, rebarItemOffHand)
                }
            }
        }
    }
}