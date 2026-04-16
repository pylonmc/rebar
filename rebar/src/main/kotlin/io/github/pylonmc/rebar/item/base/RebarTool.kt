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
import org.jetbrains.annotations.ApiStatus

interface RebarTool {
    /**
     * Called when the item is used to damage a block.
     */
    fun onUsedToDamageBlock(event: BlockDamageEvent, priority: EventPriority) {}

    /**
     * Called when the item is used to break a block.
     */
    fun onUsedToBreakBlock(event: BlockBreakEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onUsedToDamageBlock(event: BlockDamageEvent, priority: EventPriority) {
            val rebarItem = RebarItem.fromStack(event.itemInHand, RebarTool::class.java)
            if (rebarItem !is RebarItem) return
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
            val rebarItemMainHand = RebarItem.fromStack(event.player.inventory.itemInMainHand, RebarTool::class.java)
            if (rebarItemMainHand is RebarItem) {
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

            val rebarItemOffHand = RebarItem.fromStack(event.player.inventory.itemInOffHand, RebarTool::class.java)
            if (rebarItemOffHand is RebarItem) {
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