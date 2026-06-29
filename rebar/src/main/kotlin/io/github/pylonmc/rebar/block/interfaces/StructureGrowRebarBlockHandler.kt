package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.world.StructureGrowEvent
import org.jetbrains.annotations.ApiStatus

interface StructureGrowRebarBlockHandler {
    fun onStructureGrow(event: StructureGrowEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onStructureGrow(event: StructureGrowEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.location)
            if (rebarBlock is StructureGrowRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onStructureGrow", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            } else if (priority == EventPriority.LOWEST) {
                event.isCancelled = true
            }
        }
    }
}