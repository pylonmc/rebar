package io.github.pylonmc.rebar.item.interfaces

import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerDropItemEvent
import org.jetbrains.annotations.ApiStatus

interface DropRebarItemHandler {
    fun onDrop(event: PlayerDropItemEvent, priority: EventPriority)

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onDrop(event: PlayerDropItemEvent, priority: EventPriority) {
            val rebarItem = RebarItem.fromStack(event.itemDrop.itemStack, DropRebarItemHandler::class.java)
            val droppable = rebarItem as? RebarItem ?: return

            try {
                MultiHandlers.handleEvent(droppable, "onDrop", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }
    }
}