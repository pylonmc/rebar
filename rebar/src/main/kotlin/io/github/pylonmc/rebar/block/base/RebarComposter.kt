package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.papermc.paper.event.block.CompostItemEvent
import io.papermc.paper.event.entity.EntityCompostItemEvent
import org.bukkit.event.EventPriority

interface RebarComposter {
    fun onCompostByHopper(event: CompostItemEvent, priority: EventPriority) {}
    fun onCompostByEntity(event: EntityCompostItemEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onCompostByHopper(event: CompostItemEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarComposter) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onCompostByHopper", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onCompostByEntity(event: EntityCompostItemEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarComposter) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onCompostByEntity", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}