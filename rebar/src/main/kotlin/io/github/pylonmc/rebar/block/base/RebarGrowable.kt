package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockFertilizeEvent
import org.bukkit.event.block.BlockGrowEvent

interface RebarGrowable {
    fun onGrow(event: BlockGrowEvent, priority: EventPriority) {}
    fun onFertilize(event: BlockFertilizeEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onBlockGrow(event: BlockGrowEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarGrowable) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onGrow", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onBlockFertilize(event: BlockFertilizeEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarGrowable) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onFertilize", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}