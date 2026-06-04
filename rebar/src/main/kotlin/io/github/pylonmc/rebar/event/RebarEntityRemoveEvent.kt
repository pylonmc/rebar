package io.github.pylonmc.rebar.event

import io.github.pylonmc.rebar.entity.RebarEntity
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.event.entity.EntityRemoveEvent

/**
 * Called when a [RebarEntity] is removed for any reason.
 */
class RebarEntityRemoveEvent(val rebarEntity: RebarEntity<*>, val event: EntityRemoveEvent) : Event() {

    override fun getHandlers(): HandlerList
        = handlerList

    companion object {
        @JvmStatic
        val handlerList: HandlerList = HandlerList()
    }
}