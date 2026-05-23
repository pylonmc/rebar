package io.github.pylonmc.rebar.item.base

import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.PrepareAnvilEvent

/**
 * Enables the use of a RebarItem as if it's its vanilla counterpart in an anvil
 * For example: If you have a rebar item that's an enchantment book, and you want it to be usable in an anvil
 */
interface VanillaAnvilItem {
    fun onPrepareAnvilCraft(event: PrepareAnvilEvent) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onPrepareAnvil(event: PrepareAnvilEvent, priority: EventPriority) {
            val rebarItem1 = RebarItem.fromStack(event.inventory.firstItem)
            val rebarItem2 = RebarItem.fromStack(event.inventory.secondItem)
            if (rebarItem1 is VanillaAnvilItem) {
                try {
                    MultiHandlers.handleEvent(rebarItem1, "onPrepareAnvilCraft", event, priority)
                } catch (e: Exception) {
                    RebarItemListener.logEventHandleErr(event, e, rebarItem1)
                }
            }
            if (rebarItem2 is VanillaAnvilItem) {
                try {
                    MultiHandlers.handleEvent(rebarItem2, "onPrepareAnvilCraft", event, priority)
                } catch (e: Exception) {
                    RebarItemListener.logEventHandleErr(event, e, rebarItem2)
                }
            }
        }
    }
}