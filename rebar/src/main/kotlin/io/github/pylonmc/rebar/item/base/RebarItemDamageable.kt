package io.github.pylonmc.rebar.item.base

import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import io.github.pylonmc.rebar.item.research.Research.Companion.canUse
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.event.player.PlayerItemMendEvent

interface RebarItemDamageable {
    /**
     * Called when the item is damaged (loses durability).
     */
    fun onItemDamaged(event: PlayerItemDamageEvent, priority: EventPriority) {}

    /**
     * Called when the item is broken.
     */
    fun onItemBreaks(event: PlayerItemBreakEvent, priority: EventPriority) {}

    /**
     * Called when the item is mended (gains durability).
     */
    fun onItemMended(event: PlayerItemMendEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onItemDamaged(event: PlayerItemDamageEvent, priority: EventPriority) {
            val rebarItem = RebarItem.fromStack(event.item)
            if (rebarItem !is RebarItemDamageable) return
            if (!event.player.canUse(rebarItem, false)) {
                return
            }

            try {
                MultiHandlers.handleEvent(rebarItem, "onItemDamaged", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }

        @UniversalHandler
        private fun onItemBreaks(event: PlayerItemBreakEvent, priority: EventPriority) {
            val rebarItem = RebarItem.fromStack(event.brokenItem)
            if (rebarItem !is RebarItemDamageable) return
            if (!event.player.canUse(rebarItem, false)) {
                return
            }

            try {
                MultiHandlers.handleEvent(rebarItem, "onItemBreaks", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }

        @UniversalHandler
        private fun onItemMended(event: PlayerItemMendEvent, priority: EventPriority) {
            val rebarItem = RebarItem.fromStack(event.item)
            if (rebarItem !is RebarItemDamageable) return
            if (!event.player.canUse(rebarItem, false)) {
                event.isCancelled = true
                return
            }

            try {
                MultiHandlers.handleEvent(rebarItem, "onItemMended", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }
    }
}