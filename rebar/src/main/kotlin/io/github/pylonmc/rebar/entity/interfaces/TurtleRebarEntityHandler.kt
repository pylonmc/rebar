package io.github.pylonmc.rebar.entity.interfaces

import com.destroystokyo.paper.event.entity.TurtleGoHomeEvent
import com.destroystokyo.paper.event.entity.TurtleLayEggEvent
import com.destroystokyo.paper.event.entity.TurtleStartDiggingEvent
import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.jetbrains.annotations.ApiStatus

interface TurtleRebarEntityHandler {
    fun onTurtleStartDigging(event: TurtleStartDiggingEvent, priority: EventPriority) {}
    fun onTurtleGoHome(event: TurtleGoHomeEvent, priority: EventPriority) {}
    fun onTurtleLayEgg(event: TurtleLayEggEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onTurtleStartDigging(event: TurtleStartDiggingEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is TurtleRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onTurtleStartDigging", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onTurtleGoHome(event: TurtleGoHomeEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is TurtleRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onTurtleGoHome", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onTurtleLayEgg(event: TurtleLayEggEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is TurtleRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onTurtleLayEgg", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}