package io.github.pylonmc.rebar.item.interfaces

import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockDispenseEvent
import org.jetbrains.annotations.ApiStatus

interface DispenseRebarItemHandler {
    fun onDispensed(event: BlockDispenseEvent, priority: EventPriority)

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onDispensed(event: BlockDispenseEvent, priority: EventPriority) {
            val rebarItem = RebarItem.fromStack(event.item, DispenseRebarItemHandler::class.java)
            val dispensable = rebarItem as? RebarItem ?: return

            try {
                MultiHandlers.handleEvent(dispensable, "onDispensed", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }
    }
}