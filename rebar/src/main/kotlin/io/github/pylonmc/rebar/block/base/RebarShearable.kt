package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.papermc.paper.event.block.PlayerShearBlockEvent
import org.bukkit.event.EventPriority

interface RebarShearable {
    fun onShear(event: PlayerShearBlockEvent, priority: EventPriority)

    companion object : MultiListener {
        @UniversalHandler
        private fun onShearBlock(event: PlayerShearBlockEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarShearable) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onShear", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}