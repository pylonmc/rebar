package io.github.pylonmc.rebar.item.base

import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import io.github.pylonmc.rebar.item.research.Research.Companion.canUse
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerInteractEvent

interface RebarBlockInteractor : RebarCooldownable {
    /**
     * May be fired twice (once for each hand), and is fired for both left and right clicks.
     */
    fun onUsedToClickBlock(event: PlayerInteractEvent, priority: EventPriority)

    companion object : MultiListener {
        @UniversalHandler
        private fun onUsedToClickBlock(event: PlayerInteractEvent, priority: EventPriority) {
            if (!event.hasBlock()) return
            val rebarItem = event.item?.let { RebarItem.fromStack(it) } ?: return
            if (rebarItem !is RebarBlockInteractor) return
            if (!event.player.canUse(rebarItem, false)) {
                event.setUseItemInHand(Event.Result.DENY)
                return
            } else if (rebarItem.respectCooldown && event.player.getCooldown(rebarItem.stack) > 0) {
                event.setUseItemInHand(Event.Result.DENY)
                return
            } else if (priority == EventPriority.LOWEST && !rebarItem.respectCooldown && event.player.getCooldown(rebarItem.stack) > 0 && event.useItemInHand() == Event.Result.DENY) {
                // Bukkit by default sets use item in hand to DENY if the item is on cooldown, but if we don't respect the cooldown, we want to allow it being used
                event.setUseItemInHand(Event.Result.ALLOW)
            }

            try {
                MultiHandlers.handleEvent(rebarItem, "onUsedToClickBlock", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }
    }
}