package io.github.pylonmc.rebar.item.interfaces

import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.PrepareAnvilEvent

interface AnvilUseRebarItemHandler {
    fun onPrepareAnvil(event: PrepareAnvilEvent) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onPrepareAnvil(event: PrepareAnvilEvent, priority: EventPriority) {
            val firstRebarItem = RebarItem.fromStack(event.inventory.firstItem, AnvilUseRebarItemHandler::class.java)
            if (firstRebarItem is RebarItem) {
                try {
                    MultiHandlers.handleEvent(firstRebarItem, "onPrepareAnvil", event, priority)
                } catch (e: Exception) {
                    RebarItemListener.logEventHandleErr(event, e, firstRebarItem)
                }
            }

            val secondRebarItem = RebarItem.fromStack(event.inventory.secondItem, AnvilUseRebarItemHandler::class.java)
            if (secondRebarItem is RebarItem) {
                try {
                    MultiHandlers.handleEvent(secondRebarItem, "onPrepareAnvil", event, priority)
                } catch (e: Exception) {
                    RebarItemListener.logEventHandleErr(event, e, secondRebarItem)
                }
            }
        }
    }
}