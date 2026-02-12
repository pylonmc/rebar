package io.github.pylonmc.rebar.item.base

import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import io.github.pylonmc.rebar.item.research.Research.Companion.canUse
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerInteractEvent

interface RebarInteractor : RebarCooldownable {
    /**
     * Called when a player right clicks with the item in either main or off hand.
     */
    fun onUsedToClick(event: PlayerInteractEvent, priority: EventPriority)

    companion object : MultiListener {
        @UniversalHandler
        private fun onUsedToClick(event: PlayerInteractEvent, priority: EventPriority) {
            val rebarItem = event.item?.let { RebarItem.fromStack(it) } ?: return
            if (rebarItem !is RebarInteractor) return
            if (!event.player.canUse(rebarItem, false)) {
                event.isCancelled = true
                return
            } else if (rebarItem.respectCooldown && event.player.getCooldown(rebarItem.stack) > 0) {
                event.isCancelled = true
                return
            }

            try {
                MultiHandler.handleEvent(rebarItem, "onUsedToClick", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }
    }
}