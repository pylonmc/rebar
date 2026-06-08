package io.github.pylonmc.rebar.item.interfaces

import com.destroystokyo.paper.event.player.PlayerReadyArrowEvent
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import io.github.pylonmc.rebar.item.research.Research.Companion.canUse
import org.bukkit.event.EventPriority
import org.jetbrains.annotations.ApiStatus

interface ArrowRebarItemHandler : ProjectileRebarItemHandler {
    /**
     * Called when this arrow is selected for a player to fire from a bow.
     */
    fun onArrowReady(event: PlayerReadyArrowEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onArrowReady(event: PlayerReadyArrowEvent, priority: EventPriority) {
            val arrow = RebarItem.fromStack(event.arrow, ArrowRebarItemHandler::class.java)
            if (arrow !is RebarItem) return
            if (!event.player.canUse(arrow, false)) {
                event.isCancelled = true
                return
            }

            try {
                MultiHandlers.handleEvent(arrow, "onArrowReady", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, arrow)
            }
        }
    }
}