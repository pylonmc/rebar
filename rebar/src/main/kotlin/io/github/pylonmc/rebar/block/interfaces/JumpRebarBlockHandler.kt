package io.github.pylonmc.rebar.block.interfaces

import com.destroystokyo.paper.event.player.PlayerJumpEvent
import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.jetbrains.annotations.ApiStatus

interface JumpRebarBlockHandler {
    fun onJumpedOn(event: PlayerJumpEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        /**
         * TODO: Rework the logic for determining the block underneath the player, out of scope for this PR but a worthy improvement
         */
        @UniversalHandler
        private fun onJumpedOn(event: PlayerJumpEvent, priority: EventPriority) {
            val blockIn = event.player.location.block
            val blockUnder = blockIn.getRelative(0, -1, 0)
            val rebarBlock = BlockStorage.get(blockUnder) ?: BlockStorage.get(blockIn)
            if (rebarBlock is JumpRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onJumpedOn", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}