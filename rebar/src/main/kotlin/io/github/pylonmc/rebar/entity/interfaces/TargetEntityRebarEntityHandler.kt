package io.github.pylonmc.rebar.entity.interfaces

import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityTargetEvent

interface TargetEntityRebarEntityHandler {
    fun onTargetEntity(event: EntityTargetEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onTarget(event: EntityTargetEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is PathfindRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onTargetEntity", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}