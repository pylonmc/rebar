package io.github.pylonmc.rebar.event

import io.github.pylonmc.rebar.electricity.ElectricityManager
import io.github.pylonmc.rebar.electricity.nodes.ElectricNode
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Called right before an [ElectricNode] is about to be removed to the [ElectricityManager]
 */
class RebarElectricNodeRemoveEvent(val node: ElectricNode) : Event() {

    override fun getHandlers(): HandlerList
        = handlerList

    companion object {
        @JvmStatic
        val handlerList: HandlerList = HandlerList()
    }
}