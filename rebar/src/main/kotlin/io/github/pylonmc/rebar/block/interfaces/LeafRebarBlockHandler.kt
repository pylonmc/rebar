package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.LeavesDecayEvent
import org.jetbrains.annotations.ApiStatus

interface LeafRebarBlockHandler {
    fun onLeafDecay(event: LeavesDecayEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onLeafDecay(event: LeavesDecayEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is LeafRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onLeafDecay", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}