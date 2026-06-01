package io.github.pylonmc.rebar.item.interfaces

import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import io.github.pylonmc.rebar.item.research.Research.Companion.canUse
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.jetbrains.annotations.ApiStatus

interface EntityInteractRebarItemHandler : CooldownRebarItem {
    /**
     * Called when a player right clicks an entity while holding the item.
     */
    fun onInteractWithEntity(event: PlayerInteractAtEntityEvent, priority: EventPriority)

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onInteractWithEntity(event: PlayerInteractAtEntityEvent, priority: EventPriority) {
            val player = event.player
            val rebarItem = RebarItem.fromStack(player.inventory.getItem(event.hand), EntityInteractRebarItemHandler::class.java)
            if (rebarItem !is RebarItem) return

            if (!player.canUse(rebarItem, false) || rebarItem.hasCooldown(player)) {
                event.isCancelled = true
                return
            }

            try {
                MultiHandlers.handleEvent(rebarItem, "onInteractWithEntity", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }
    }
}