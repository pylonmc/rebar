package io.github.pylonmc.rebar.entity.base

import com.destroystokyo.paper.event.entity.CreeperIgniteEvent
import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.CreeperPowerEvent

interface RebarCreeper {
    fun onIgnite(event: CreeperIgniteEvent, priority: EventPriority) {}
    fun onPower(event: CreeperPowerEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onCreeperIgnite(event: CreeperIgniteEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarCreeper) {
                try {
                    MultiHandler.handleEvent(rebarEntity, "onIgnite", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onCreeperPower(event: CreeperPowerEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarCreeper) {
                try {
                    MultiHandler.handleEvent(rebarEntity, "onPower", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}