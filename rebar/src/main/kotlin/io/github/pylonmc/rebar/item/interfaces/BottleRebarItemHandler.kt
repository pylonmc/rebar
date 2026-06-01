package io.github.pylonmc.rebar.item.interfaces

import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.ExpBottleEvent
import org.jetbrains.annotations.ApiStatus

interface BottleRebarItemHandler : ProjectileRebarItemHandler {
    fun onBottleBreak(event: ExpBottleEvent)

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onBottleBreak(event: ExpBottleEvent, priority: EventPriority) {
            val rebarItem = RebarItem.fromStack(event.entity.item)
            if (rebarItem !is BottleRebarItemHandler) return
            try {
                MultiHandlers.handleEvent(rebarItem, "onBottleBreak", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }
    }
}