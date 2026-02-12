package io.github.pylonmc.rebar.item.base

import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import io.github.pylonmc.rebar.item.research.Research.Companion.canUse
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerInteractEntityEvent

interface RebarItemEntityInteractor : RebarCooldownable {
    /**
     * Called when a player right clicks an entity while holding the item.
     */
    fun onUsedToRightClickEntity(event: PlayerInteractEntityEvent, priority: EventPriority)

    companion object : MultiListener {
        @UniversalHandler
        private fun onUsedToRightClickEntity(event: PlayerInteractEntityEvent, priority: EventPriority) {
            val rebarItemMainHand = RebarItem.fromStack(event.player.inventory.itemInMainHand)
            if (rebarItemMainHand is RebarItemEntityInteractor) {
                if (!event.player.canUse(rebarItemMainHand, false)) {
                    event.isCancelled = true
                } else if (rebarItemMainHand.respectCooldown && event.player.getCooldown(rebarItemMainHand.stack) > 0) {
                    event.isCancelled = true
                } else {
                    try {
                        MultiHandler.handleEvent(rebarItemMainHand, RebarItemEntityInteractor::class.java, "onUsedToRightClickEntity", event, priority)
                    } catch (e: Exception) {
                        RebarItemListener.logEventHandleErr(event, e, rebarItemMainHand)
                    }
                }
            }

            val rebarItemOffHand = RebarItem.fromStack(event.player.inventory.itemInOffHand)
            if (rebarItemOffHand is RebarItemEntityInteractor) {
                if (!event.player.canUse(rebarItemOffHand, false)) {
                    event.isCancelled = true
                } else if (rebarItemOffHand.respectCooldown && event.player.getCooldown(rebarItemOffHand.stack) > 0) {
                    event.isCancelled = true
                } else {
                    try {
                        MultiHandler.handleEvent(rebarItemOffHand, RebarItemEntityInteractor::class.java, "onUsedToRightClickEntity", event, priority)
                    } catch (e: Exception) {
                        RebarItemListener.logEventHandleErr(event, e, rebarItemOffHand)
                    }
                }
            }
        }
    }
}