package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent

interface RebarPiston {
    fun onExtend(event: BlockPistonExtendEvent, priority: EventPriority) {}
    fun onRetract(event: BlockPistonRetractEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onPistonExtend(event: BlockPistonExtendEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarPiston) {
                try {
                    MultiHandler.handleEvent(rebarBlock, "onExtend", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onPistonRetract(event: BlockPistonRetractEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarPiston) {
                try {
                    MultiHandler.handleEvent(rebarBlock, "onRetract", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}