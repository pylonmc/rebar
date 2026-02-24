package io.github.pylonmc.rebar.item.base

import com.destroystokyo.paper.event.player.PlayerReadyArrowEvent
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import io.github.pylonmc.rebar.item.research.Research.Companion.canUse
import org.bukkit.entity.AbstractArrow
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.ProjectileHitEvent

interface RebarArrow {
    /**
     * Called when this arrow is selected for a player to fire from a bow.
     */
    fun onArrowReady(event: PlayerReadyArrowEvent, priority: EventPriority) {}

    /**
     * Called when the arrow is shot from the bow of any entity.
     */
    fun onArrowShotFromBow(event: EntityShootBowEvent, priority: EventPriority) {}

    /**
     * Called when the arrow hits any target, including blocks and entities.
     */
    fun onArrowHit(event: ProjectileHitEvent, priority: EventPriority) {}

    /**
     * Called when the arrow damages an entity.
     */
    fun onArrowDamage(event: EntityDamageByEntityEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onArrowReady(event: PlayerReadyArrowEvent, priority: EventPriority) {
            val arrow = RebarItem.fromStack(event.arrow)
            if (arrow !is RebarArrow) return
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

        @UniversalHandler
        private fun onArrowShotFromBow(event: EntityShootBowEvent, priority: EventPriority) {
            val arrow = event.consumable?.let { RebarItem.fromStack(it) }
            if (arrow !is RebarArrow) return

            try {
                MultiHandlers.handleEvent(arrow, "onArrowShotFromBow", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, arrow)
            }
        }

        @UniversalHandler
        private fun onArrowHit(event: ProjectileHitEvent, priority: EventPriority) {
            if (event.entity is AbstractArrow) {
                val arrow = RebarItem.fromStack((event.entity as AbstractArrow).itemStack)
                if (arrow is RebarArrow) {
                    try {
                        MultiHandlers.handleEvent(arrow, "onArrowHit", event, priority)
                    } catch (e: Exception) {
                        RebarItemListener.logEventHandleErr(event, e, arrow)
                    }
                }
            }
        }

        @UniversalHandler
        private fun onArrowDamage(event: EntityDamageByEntityEvent, priority: EventPriority) {
            if (event.damager is AbstractArrow) {
                val arrow = RebarItem.fromStack((event.damager as AbstractArrow).itemStack)
                if (arrow is RebarArrow) {
                    try {
                        MultiHandlers.handleEvent(arrow, "onArrowDamage", event, priority)
                    } catch (e: Exception) {
                        RebarItemListener.logEventHandleErr(event, e, arrow)
                    }
                }
            }
        }
    }
}