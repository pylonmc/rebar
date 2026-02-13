package io.github.pylonmc.rebar.item.base

import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockDispenseEvent

interface RebarDispensable {
    fun onDispense(event: BlockDispenseEvent, priority: EventPriority)

    companion object : MultiListener {
        @UniversalHandler
        private fun onDispense(event: BlockDispenseEvent, priority: EventPriority) {
            val rebarItem = RebarItem.fromStack(event.item)
            val dispensable = rebarItem as? RebarDispensable ?: return

            try {
                MultiHandler.handleEvent(dispensable, "onDispense", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }
    }
}