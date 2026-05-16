package io.github.pylonmc.rebar.item.base

import io.github.pylonmc.rebar.item.RebarItem
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

/**
 * Represents a [RebarItem] that has special behavior when a player joins the server.
 * This interface allows items to perform actions or update their state when a player joins.
 */
interface RebarJoinHandler {
    /**
     * Called when the player joins the server
     */
    fun onRejoin(event: PlayerJoinEvent)

    companion object : Listener {
        @EventHandler
        fun onRejoin(event : PlayerJoinEvent) {
            for (item in event.player.inventory) {
                val rebarItem = RebarItem.fromStack(item)
                if (rebarItem is RebarJoinHandler) {
                    rebarItem.onRejoin(event)
                }
            }
        }
    }
}