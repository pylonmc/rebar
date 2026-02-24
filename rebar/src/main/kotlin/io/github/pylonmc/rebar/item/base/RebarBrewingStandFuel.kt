package io.github.pylonmc.rebar.item.base

import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.BrewingStandFuelEvent

interface RebarBrewingStandFuel {
    /**
     * Called when the item is consumed as fuel in a brewing stand.
     */
    fun onUsedAsBrewingStandFuel(event: BrewingStandFuelEvent, priority: EventPriority)

    companion object : MultiListener {
        @UniversalHandler
        private fun onUsedAsBrewingStandFuel(event: BrewingStandFuelEvent, priority: EventPriority) {
            val rebarItem = RebarItem.fromStack(event.fuel)
            if (rebarItem !is RebarBrewingStandFuel) return

            try {
                MultiHandlers.handleEvent(rebarItem, "onUsedAsBrewingStandFuel", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }
    }
}