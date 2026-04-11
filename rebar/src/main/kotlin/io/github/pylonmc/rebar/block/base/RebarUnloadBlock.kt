package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.event.RebarBlockUnloadEvent
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.jetbrains.annotations.ApiStatus

interface RebarUnloadBlock {
    fun onUnload(event: RebarBlockUnloadEvent, priority: EventPriority)

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onUnload(event: RebarBlockUnloadEvent, priority: EventPriority) {
            val rebarBlock = event.rebarBlock
            if (rebarBlock is RebarUnloadBlock) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onUnload", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, event.rebarBlock)
                }
            }
        }
    }
}