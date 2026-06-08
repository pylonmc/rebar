package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.papermc.paper.event.player.PlayerFlowerPotManipulateEvent
import org.bukkit.event.EventPriority
import org.jetbrains.annotations.ApiStatus

interface FlowerPotRebarBlockHandler {
    fun onFlowerPotManipulate(event: PlayerFlowerPotManipulateEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onFlowerPotManipulate(event: PlayerFlowerPotManipulateEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.flowerpot)
            if (rebarBlock is FlowerPotRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onFlowerPotManipulate", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}