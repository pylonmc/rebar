package io.github.pylonmc.rebar.event

import io.github.pylonmc.rebar.electricity.ElectricityManager
import io.github.pylonmc.rebar.electricity.nodes.ElectricNode
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Called after an [ElectricNode] has been added to the [ElectricityManager]
 */
class RebarElectricNodeAddEvent(val node: ElectricNode) : Event() {

    override fun getHandlers(): HandlerList
        = handlerList

    companion object {
        @JvmStatic
        val handlerList: HandlerList = HandlerList()
    }
}