package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.papermc.paper.event.block.TargetHitEvent
import org.bukkit.event.EventPriority
import org.jetbrains.annotations.ApiStatus

interface TargetRebarBlockHandler {
    fun onTargetHit(event: TargetHitEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onTargetHit(event: TargetHitEvent, priority: EventPriority) {
            val hitBlock = event.hitBlock ?: return
            val rebarBlock = BlockStorage.get(hitBlock)
            if (rebarBlock is TargetRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onTargetHit", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}