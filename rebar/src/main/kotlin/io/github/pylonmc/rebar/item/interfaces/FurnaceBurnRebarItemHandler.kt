package io.github.pylonmc.rebar.item.interfaces

import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.FurnaceBurnEvent
import org.jetbrains.annotations.ApiStatus

interface FurnaceBurnRebarItemHandler {
    /**
     * Called when the item is burnt as fuel in a furnace, smoker, or blast furnace.
     */
    fun onItemBurnByFurnace(event: FurnaceBurnEvent, priority: EventPriority)

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onItemBurnByFurnace(event: FurnaceBurnEvent, priority: EventPriority) {
            val rebarItem = RebarItem.fromStack(event.fuel, FurnaceBurnRebarItemHandler::class.java) ?: return
            if (rebarItem !is RebarItem) return
            try {
                MultiHandlers.handleEvent(rebarItem, "onItemBurnByFurnace", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }
    }
}