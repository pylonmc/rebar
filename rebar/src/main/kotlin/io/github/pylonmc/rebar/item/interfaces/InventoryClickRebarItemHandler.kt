package io.github.pylonmc.rebar.item.interfaces

import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryClickEvent
import org.jetbrains.annotations.ApiStatus

interface InventoryClickRebarItemHandler {

    /**
     * Called when the item is clicked on while being in a slot
     */
    fun onClickedInSlot(event: InventoryClickEvent, priority: EventPriority) {}

    /**
     * Called when the item is currently on the cursor and the cursor has clicked
     */
    fun onClickedInCursor(event: InventoryClickEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onInventoryClick(event: InventoryClickEvent, priority: EventPriority) {
            run {
                val rebarItem = RebarItem.fromStack(event.currentItem, InventoryClickRebarItemHandler::class.java)
                val item = rebarItem as? RebarItem ?: return@run

                try {
                    MultiHandlers.handleEvent(item, "onClickedInSlot", event, priority)
                } catch (e: Exception) {
                    RebarItemListener.logEventHandleErr(event, e, rebarItem)
                }
            }

            run {
                val rebarItem = RebarItem.fromStack(event.cursor, InventoryClickRebarItemHandler::class.java)
                val item = rebarItem as? RebarItem ?: return@run

                try {
                    MultiHandlers.handleEvent(item, "onClickedInCursor", event, priority)
                } catch (e: Exception) {
                    RebarItemListener.logEventHandleErr(event, e, rebarItem)
                }
            }
        }
    }
}