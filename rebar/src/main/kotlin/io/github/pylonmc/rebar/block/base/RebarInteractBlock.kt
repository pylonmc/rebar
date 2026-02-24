package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerInteractEvent

interface RebarInteractBlock {

    /**
     * This may be called for both hands, so make sure you check which hand is used.
     */
    fun onInteract(event: PlayerInteractEvent, priority: EventPriority)

    companion object : MultiListener {
        @UniversalHandler
        private fun onPlayerBlockInteract(event: PlayerInteractEvent, priority: EventPriority) {
            val clickedBlock = event.clickedBlock ?: return
            val rebarBlock = BlockStorage.get(clickedBlock)
            if (rebarBlock is RebarInteractBlock) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onInteract", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}