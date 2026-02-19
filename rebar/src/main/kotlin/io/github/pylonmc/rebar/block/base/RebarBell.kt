package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BellResonateEvent
import org.bukkit.event.block.BellRingEvent

interface RebarBell {
    fun onRing(event: BellRingEvent, priority: EventPriority) {}
    fun onResonate(event: BellResonateEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onBellRing(event: BellRingEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarBell) {
                try {
                    MultiHandler.handleEvent(rebarBlock, "onRing", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onBellResonate(event: BellResonateEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarBell) {
                try {
                    MultiHandler.handleEvent(rebarBlock, "onResonate", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}