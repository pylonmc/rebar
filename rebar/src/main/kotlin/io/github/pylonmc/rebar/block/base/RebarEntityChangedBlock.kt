package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityChangeBlockEvent

interface RebarEntityChangedBlock {
    fun onEntityChanged(event: EntityChangeBlockEvent, priority: EventPriority)

    companion object : MultiListener {
        @UniversalHandler
        private fun onEntityChangeBlock(event: EntityChangeBlockEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarEntityChangedBlock) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onEntityChanged", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}