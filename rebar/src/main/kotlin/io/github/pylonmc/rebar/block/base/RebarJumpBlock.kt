package io.github.pylonmc.rebar.block.base

import com.destroystokyo.paper.event.player.PlayerJumpEvent
import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority

interface RebarJumpBlock {
    fun onJumpedOn(event: PlayerJumpEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onPlayerJumpEvent(event: PlayerJumpEvent, priority: EventPriority) {
            val blockUnder = event.player.location.add(0.0, -1.0, 0.0).block
            val blockIn = event.player.location.add(0.0, 0.0, 0.0).block
            val rebarBlock = BlockStorage.get(blockUnder) ?: BlockStorage.get(blockIn)
            if (rebarBlock is RebarJumpBlock) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onJumpedOn", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}