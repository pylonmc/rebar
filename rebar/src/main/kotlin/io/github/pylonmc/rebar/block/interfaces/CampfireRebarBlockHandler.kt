package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockCookEvent
import org.bukkit.event.block.CampfireStartEvent
import org.jetbrains.annotations.ApiStatus

interface CampfireRebarBlockHandler {
    fun onCampfireStart(event: CampfireStartEvent, priority: EventPriority) {}
    fun onCampfireCook(event: BlockCookEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onCampfireStart(event: CampfireStartEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is CampfireRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onCampfireStart", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onCampfireCook(event: BlockCookEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is CampfireRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onCampfireCook", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}