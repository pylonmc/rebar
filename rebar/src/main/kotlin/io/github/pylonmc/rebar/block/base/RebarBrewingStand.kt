package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockCookEvent
import org.bukkit.event.block.InventoryBlockStartEvent
import org.bukkit.event.inventory.BrewingStandFuelEvent

interface RebarBrewingStand {
    fun onStartBrewing(event: InventoryBlockStartEvent, priority: EventPriority) {}
    fun onFuel(event: BrewingStandFuelEvent, priority: EventPriority) {}
    fun onEndBrewing(event: BlockCookEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onStartCook(event: InventoryBlockStartEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarBrewingStand) {
                try {
                    MultiHandler.handleEvent(rebarBlock, "onStartBrewing", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onBrewingStandFuel(event: BrewingStandFuelEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarBrewingStand) {
                try {
                    MultiHandler.handleEvent(rebarBlock, "onFuel", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onFinishCook(event: BlockCookEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarBrewingStand) {
                try {
                    MultiHandler.handleEvent(rebarBlock, "onEndBrewing", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}