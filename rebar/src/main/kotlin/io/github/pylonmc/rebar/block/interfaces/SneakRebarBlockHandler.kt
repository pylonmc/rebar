package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.jetbrains.annotations.ApiStatus

interface SneakRebarBlockHandler {
    fun onSneakStart(event: PlayerToggleSneakEvent, priority: EventPriority) {}
    fun onSneakStop(event: PlayerToggleSneakEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        /**
         * TODO: Rework the logic for determining the block underneath the player, out of scope for this PR but a worthy improvement
         */
        @UniversalHandler
        private fun onPlayerToggleSneak(event: PlayerToggleSneakEvent, priority: EventPriority) {
            val blockIn = event.player.location.block
            val blockUnder = blockIn.getRelative(0, -1, 0)
            val rebarBlock = BlockStorage.get(blockUnder) ?: BlockStorage.get(blockIn)
            if (rebarBlock is SneakRebarBlockHandler) {
                /*
                 * Event player is from before the event is triggered, so when the player
                 * is marked as *not* sneaking, they just toggled it.
                 */
                if (!event.player.isSneaking) {
                    try {
                        MultiHandlers.handleEvent(rebarBlock, "onSneakStart", event, priority)
                    } catch (e: Exception) {
                        BlockListener.logEventHandleErr(event, e, rebarBlock)
                    }
                } else {
                    try {
                        MultiHandlers.handleEvent(rebarBlock, "onSneakStop", event, priority)
                    } catch (e: Exception) {
                        BlockListener.logEventHandleErr(event, e, rebarBlock)
                    }
                }
            }
        }
    }
}