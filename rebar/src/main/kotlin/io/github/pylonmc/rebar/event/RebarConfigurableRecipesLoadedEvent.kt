package io.github.pylonmc.rebar.event

import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Called when all the configurable recipes are loaded
 */
class RebarConfigurableRecipesLoadedEvent: Event() {
    override fun getHandlers(): HandlerList
            = handlerList

    companion object {
        @JvmStatic
        val handlerList: HandlerList = HandlerList()
    }
}