package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerToggleSneakEvent

interface RebarSneakableBlock {
    fun onSneakedOn(event: PlayerToggleSneakEvent, priority: EventPriority) {}
    fun onUnsneakedOn(event: PlayerToggleSneakEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onPlayerToggleSneak(event: PlayerToggleSneakEvent, priority: EventPriority) {
            val blockUnder = event.player.location.add(0.0, -1.0, 0.0).block
            val blockIn = event.player.location.add(0.0, 0.0, 0.0).block
            val rebarBlock = BlockStorage.get(blockUnder) ?: BlockStorage.get(blockIn)
            if (rebarBlock is RebarSneakableBlock) {
                /*
                 * Event player is from before the event is triggered, so when the player
                 * is marked as *not* sneaking, they just toggled it.
                 */
                if (!event.player.isSneaking) {
                    try {
                        MultiHandler.handleEvent(rebarBlock, "onSneakedOn", event, priority)
                    } catch (e: Exception) {
                        BlockListener.logEventHandleErr(event, e, rebarBlock)
                    }
                } else {
                    try {
                        MultiHandler.handleEvent(rebarBlock, "onUnsneakedOn", event, priority)
                    } catch (e: Exception) {
                        BlockListener.logEventHandleErr(event, e, rebarBlock)
                    }
                }
            }
        }
    }
}