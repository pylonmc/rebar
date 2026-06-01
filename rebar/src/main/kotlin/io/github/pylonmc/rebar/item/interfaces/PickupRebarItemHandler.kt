package io.github.pylonmc.rebar.item.interfaces

import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerAttemptPickupItemEvent
import org.jetbrains.annotations.ApiStatus

interface PickupRebarItemHandler {
    fun onItemPickupAttempt(event: PlayerAttemptPickupItemEvent, priority: EventPriority)

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onItemPickupAttempt(event: PlayerAttemptPickupItemEvent, priority: EventPriority) {
            val rebarItem = RebarItem.fromStack(event.item.itemStack, PickupRebarItemHandler::class.java)
            val pickupable = rebarItem as? RebarItem ?: return

            try {
                MultiHandlers.handleEvent(pickupable, "onItemPickupAttempt", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }
    }
}