package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.papermc.paper.event.block.PlayerShearBlockEvent
import org.bukkit.event.EventPriority
import org.jetbrains.annotations.ApiStatus

/**
 * Called when the player *right clicks* a block with shears - such as a pumpkin to turn it into a carved pumpkin.
 */
interface ShearRebarBlockHandler {
    fun onSheared(event: PlayerShearBlockEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onSheared(event: PlayerShearBlockEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is ShearRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onSheared", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}