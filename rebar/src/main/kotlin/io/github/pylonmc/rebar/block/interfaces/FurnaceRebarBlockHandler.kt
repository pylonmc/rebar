package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.FurnaceBurnEvent
import org.bukkit.event.inventory.FurnaceExtractEvent
import org.bukkit.event.inventory.FurnaceSmeltEvent
import org.bukkit.event.inventory.FurnaceStartSmeltEvent
import org.jetbrains.annotations.ApiStatus

interface FurnaceRebarBlockHandler : VanillaInventoryRebarBlockHandler {
    fun onFurnaceStart(event: FurnaceStartSmeltEvent, priority: EventPriority) {}
    fun onFurnaceSmelt(event: FurnaceSmeltEvent, priority: EventPriority) {}
    fun onFurnaceExtracted(event: FurnaceExtractEvent, priority: EventPriority) {}
    fun onFurnaceBurnFuel(event: FurnaceBurnEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onFurnaceStart(event: FurnaceStartSmeltEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is FurnaceRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onFurnaceStart", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onFurnaceSmelt(event: FurnaceSmeltEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is FurnaceRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onFurnaceSmelt", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onFurnaceExtracted(event: FurnaceExtractEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is FurnaceRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onFurnaceExtracted", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onFurnaceBurnFuel(event: FurnaceBurnEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is FurnaceRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onFurnaceBurnFuel", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}