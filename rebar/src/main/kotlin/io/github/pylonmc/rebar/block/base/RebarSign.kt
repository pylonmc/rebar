package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.papermc.paper.event.player.PlayerOpenSignEvent
import org.bukkit.event.EventPriority
import org.bukkit.event.block.SignChangeEvent

interface RebarSign {
    fun onSignChange(event: SignChangeEvent, priority: EventPriority) {}
    fun onOpen(event: PlayerOpenSignEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onSignChange(event: SignChangeEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarSign) {
                try {
                    MultiHandler.handleEvent(rebarBlock, "onSignChange", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onSignOpen(event: PlayerOpenSignEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.sign.block)
            if (rebarBlock is RebarSign) {
                try {
                    MultiHandler.handleEvent(rebarBlock, "onOpen", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}