package io.github.pylonmc.rebar.item.base

import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import io.github.pylonmc.rebar.item.research.Research.Companion.canUse
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerItemConsumeEvent

interface RebarConsumable {
    /**
     * Called when the item is consumed by a player.
     */
    fun onConsumed(event: PlayerItemConsumeEvent, priority: EventPriority)

    companion object : MultiListener {
        @UniversalHandler
        private fun onConsumed(event: PlayerItemConsumeEvent, priority: EventPriority) {
            val rebarItem = RebarItem.fromStack(event.item)
            if (rebarItem !is RebarConsumable) return
            if (!event.player.canUse(rebarItem, false)) {
                event.isCancelled = true
                return
            }

            try {
                MultiHandlers.handleEvent(rebarItem, "onConsumed", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }
    }
}