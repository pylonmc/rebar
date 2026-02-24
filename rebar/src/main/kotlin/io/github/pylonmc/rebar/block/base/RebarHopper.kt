package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.block.Hopper
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryPickupItemEvent

interface RebarHopper {
    fun onHopperPickUpItem(event: InventoryPickupItemEvent, priority: EventPriority)

    companion object : MultiListener {
        @UniversalHandler
        private fun onInventoryPickup(event: InventoryPickupItemEvent, priority: EventPriority) {
            val holder = event.inventory.holder
            if (holder is Hopper) {
                val rebarBlock = BlockStorage.get(holder.block)
                if (rebarBlock is RebarHopper) {
                    try {
                        MultiHandlers.handleEvent(rebarBlock, "onHopperPickUpItem", event, priority)
                    } catch (e: Exception) {
                        BlockListener.logEventHandleErr(event, e, rebarBlock)
                    }
                }
            }
        }
    }
}