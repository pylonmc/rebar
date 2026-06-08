package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.event.RebarCargoConnectEvent
import io.github.pylonmc.rebar.event.RebarCargoDisconnectEvent
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority

interface CargoRebarBlockHandler {
    fun onCargoDuctConnect(event: RebarCargoConnectEvent, priority: EventPriority) {}
    fun onCargoDuctDisconnect(event: RebarCargoDisconnectEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onCargoDuctConnect(event: RebarCargoConnectEvent, priority: EventPriority) {
            val block1 = event.block1
            if (block1 is CargoRebarBlockHandler) {
                MultiHandlers.handleEvent(block1, "onCargoDuctConnect", event, priority)
            }
            val block2 = event.block2
            if (block2 is CargoRebarBlockHandler) {
                MultiHandlers.handleEvent(block2, "onCargoDuctConnect", event, priority)
            }
        }

        @UniversalHandler
        private fun onCargoDuctDisconnect(event: RebarCargoDisconnectEvent, priority: EventPriority) {
            val block1 = event.block1
            if (block1 is CargoRebarBlockHandler) {
                MultiHandlers.handleEvent(block1, "onCargoDuctDisconnect", event, priority)
            }
            val block2 = event.block2
            if (block2 is CargoRebarBlockHandler) {
                MultiHandlers.handleEvent(block2, "onCargoDuctDisconnect", event, priority)
            }
        }
    }
}