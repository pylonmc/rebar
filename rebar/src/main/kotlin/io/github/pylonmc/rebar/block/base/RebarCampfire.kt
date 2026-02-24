package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockCookEvent
import org.bukkit.event.block.InventoryBlockStartEvent

interface RebarCampfire {
    fun onStartCooking(event: InventoryBlockStartEvent, priority: EventPriority) {}
    fun onEndCooking(event: BlockCookEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onStartCook(event: InventoryBlockStartEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarCampfire) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onStartCooking", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onFinishCook(event: BlockCookEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarCampfire) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onEndCooking", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}