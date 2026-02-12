package io.github.pylonmc.rebar.item.base

import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.FurnaceBurnEvent

/**
 * Allows the item to act as a normal vanilla fuel.
 *
 * For example, by default, a 'magic coal' item which has a material of coal cannot
 * be burnt in furnaces. However, if your magic coal item implements this interface,
 * it will be treated the same as a normal piece of coal when burnt as fuel.
 */
interface VanillaCookingFuel {
    /**
     * Called when the item is burnt as fuel in a furnace, smoker, or blast furnace.
     */
    fun onBurntAsFuel(event: FurnaceBurnEvent, priority: EventPriority)

    companion object : MultiListener {
        @UniversalHandler
        private fun onBurntAsFuel(event: FurnaceBurnEvent, priority: EventPriority) {
            val rebarItem = RebarItem.fromStack(event.fuel) ?: return
            if (rebarItem !is VanillaCookingFuel) return
            try {
                MultiHandler.handleEvent(rebarItem, VanillaCookingFuel::class.java, "onBurntAsFuel", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }
    }
}