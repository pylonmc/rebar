package io.github.pylonmc.rebar.entity.base

import com.destroystokyo.paper.event.entity.TurtleGoHomeEvent
import com.destroystokyo.paper.event.entity.TurtleLayEggEvent
import com.destroystokyo.paper.event.entity.TurtleStartDiggingEvent
import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority

interface RebarTurtle {
    fun onStartDigging(event: TurtleStartDiggingEvent, priority: EventPriority) {}
    fun onGoHome(event: TurtleGoHomeEvent, priority: EventPriority) {}
    fun onLayEgg(event: TurtleLayEggEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onStartDigging(event: TurtleStartDiggingEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarTurtle) {
                try {
                    MultiHandler.handleEvent(rebarEntity, "onStartDigging", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onGoHome(event: TurtleGoHomeEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarTurtle) {
                try {
                    MultiHandler.handleEvent(rebarEntity, "onGoHome", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onLayEgg(event: TurtleLayEggEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarTurtle) {
                try {
                    MultiHandler.handleEvent(rebarEntity, "onLayEgg", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}