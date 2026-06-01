package io.github.pylonmc.rebar.item.interfaces

import com.destroystokyo.paper.event.player.PlayerReadyArrowEvent
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import io.github.pylonmc.rebar.item.research.Research.Companion.canUse
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityShootBowEvent
import org.jetbrains.annotations.ApiStatus

interface BowRebarItemHandler {
    /**
     * Called when an arrow is being selected to fire from this bow.
     */
    fun onBowReady(event: PlayerReadyArrowEvent, priority: EventPriority) {}

    /**
     * Called when an arrow is shot from this bow.
     */
    fun onBowShoot(event: EntityShootBowEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onBowReady(event: PlayerReadyArrowEvent, priority: EventPriority) {
            val bow = RebarItem.fromStack(event.bow, BowRebarItemHandler::class.java)
            if (bow !is RebarItem) return
            if (!event.player.canUse(bow, false)) {
                event.isCancelled = true
                return
            }

            try {
                MultiHandlers.handleEvent(bow, "onBowReady", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, bow)
            }
        }

        @UniversalHandler
        private fun onBowShoot(event: EntityShootBowEvent, priority: EventPriority) {
            val bow = event.bow?.let { RebarItem.fromStack(it, BowRebarItemHandler::class.java) }
            if (bow !is RebarItem) return
            try {
                MultiHandlers.handleEvent(bow, "onBowShoot", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, bow)
            }
        }
    }
}