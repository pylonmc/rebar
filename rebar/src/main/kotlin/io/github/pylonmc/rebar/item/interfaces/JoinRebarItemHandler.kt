package io.github.pylonmc.rebar.item.interfaces

import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.jetbrains.annotations.ApiStatus

/**
 * Allows items to run code when a player joins.
 */
interface JoinRebarItemHandler {

    /**
     * Called when the player joins the server
     */
    fun onJoin(event: PlayerJoinEvent, priority: EventPriority)

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        fun onJoin(event: PlayerJoinEvent, priority: EventPriority) {
            for (item in event.player.inventory) {
                val rebarItem = RebarItem.fromStack(item, JoinRebarItemHandler::class.java)
                val joinHandler = rebarItem as? RebarItem ?: return
                try {
                    MultiHandlers.handleEvent(joinHandler, "onJoin", event, priority)
                } catch (e: Exception) {
                    RebarItemListener.logEventHandleErr(event, e, joinHandler)
                }
            }
        }
    }
}