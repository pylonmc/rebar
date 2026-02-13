package io.github.pylonmc.rebar.item.base

import com.destroystokyo.paper.event.player.PlayerReadyArrowEvent
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import io.github.pylonmc.rebar.item.research.Research.Companion.canUse
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityShootBowEvent

interface RebarBow {
    /**
     * Called when an arrow is being selected to fire from this bow.
     */
    fun onBowReady(event: PlayerReadyArrowEvent, priority: EventPriority) {}

    /**
     * Called when an arrow is shot from this bow.
     */
    fun onBowFired(event: EntityShootBowEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onBowReady(event: PlayerReadyArrowEvent, priority: EventPriority) {
            val bow = RebarItem.fromStack(event.bow)
            if (bow !is RebarBow) return
            if (!event.player.canUse(bow, false)) {
                event.isCancelled = true
                return
            }

            try {
                MultiHandler.handleEvent(bow, "onBowReady", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, bow)
            }
        }

        @UniversalHandler
        private fun onBowFired(event: EntityShootBowEvent, priority: EventPriority) {
            val bow = event.bow?.let { RebarItem.fromStack(it) }
            if (bow !is RebarBow) return
            try {
                MultiHandler.handleEvent(bow, "onBowFired", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, bow)
            }
        }
    }
}