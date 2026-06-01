package io.github.pylonmc.rebar.item.interfaces

import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.BrewingStandFuelEvent
import org.jetbrains.annotations.ApiStatus

interface BrewingStandFuelRebarItemHandler {
    /**
     * Called when the item is consumed as fuel in a brewing stand.
     */
    fun onFuelBrewingStand(event: BrewingStandFuelEvent, priority: EventPriority)

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onFuelBrewingStand(event: BrewingStandFuelEvent, priority: EventPriority) {
            val rebarItem = RebarItem.fromStack(event.fuel, BrewingStandFuelRebarItemHandler::class.java)
            if (rebarItem !is RebarItem) return

            try {
                MultiHandlers.handleEvent(rebarItem, "onFuelBrewingStand", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }
    }
}