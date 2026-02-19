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
import org.bukkit.event.inventory.FurnaceBurnEvent
import org.bukkit.event.inventory.FurnaceExtractEvent

interface RebarFurnace {
    fun onStartSmelting(event: InventoryBlockStartEvent, priority: EventPriority) {}
    fun onEndSmelting(event: BlockCookEvent, priority: EventPriority) {}
    fun onExtractItem(event: FurnaceExtractEvent, priority: EventPriority) {}
    fun onFuelBurn(event: FurnaceBurnEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onStartCook(event: InventoryBlockStartEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarFurnace) {
                try {
                    MultiHandler.handleEvent(rebarBlock, "onStartSmelting", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onFinishCook(event: BlockCookEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarFurnace) {
                try {
                    MultiHandler.handleEvent(rebarBlock, "onEndSmelting", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onFurnaceExtract(event: FurnaceExtractEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarFurnace) {
                try {
                    MultiHandler.handleEvent(rebarBlock, "onExtractItem", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onFurnaceBurnFuel(event: FurnaceBurnEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarFurnace) {
                try {
                    MultiHandler.handleEvent(rebarBlock, "onFuelBurn", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}