package io.github.pylonmc.rebar.item.interfaces

import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import io.github.pylonmc.rebar.item.research.Research.Companion.canUse
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerInteractEvent
import org.jetbrains.annotations.ApiStatus

interface InteractRebarItemHandler : CooldownRebarItem {
    /**
     * Called when a player clicks with the item in either main or off hand.
     */
    fun onInteract(event: PlayerInteractEvent, priority: EventPriority)

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onInteract(event: PlayerInteractEvent, priority: EventPriority) {
            val player = event.player
            val rebarItem = event.item?.let { RebarItem.fromStack(it, InteractRebarItemHandler::class.java) } ?: return
            if (rebarItem !is RebarItem) return

            if (!player.canUse(rebarItem, false) || rebarItem.hasCooldown(player)) {
                event.setUseItemInHand(Event.Result.DENY)
                return
            } else if (priority == EventPriority.LOWEST && !rebarItem.respectCooldown && player.getCooldown(rebarItem.stack) > 0 && event.useItemInHand() == Event.Result.DENY) {
                // Bukkit by default sets use item in hand to DENY if the item is on cooldown, but if we don't respect the cooldown, we want to allow it being used
                event.setUseItemInHand(Event.Result.ALLOW)
            }

            try {
                MultiHandlers.handleEvent(rebarItem, "onInteract", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }
    }
}