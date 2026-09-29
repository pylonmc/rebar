package io.github.pylonmc.rebar.event

import io.github.pylonmc.rebar.electricity.WireEntity
import org.bukkit.Location
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent
import org.bukkit.event.player.PlayerInteractEvent

class RebarPlayerInteractWireEvent(
    val wire: WireEntity,
    val interaction: PlayerInteractEvent,
    val interactionPoint: Location
) : PlayerEvent(interaction.player) {

    override fun getHandlers(): HandlerList = handlerList

    companion object {
        @JvmStatic
        val handlerList: HandlerList = HandlerList()
    }
}