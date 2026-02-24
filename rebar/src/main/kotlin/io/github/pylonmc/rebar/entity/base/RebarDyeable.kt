package io.github.pylonmc.rebar.entity.base

import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.papermc.paper.event.entity.EntityDyeEvent
import org.bukkit.event.EventPriority

interface RebarDyeable {
    fun onDye(event: EntityDyeEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onDye(event: EntityDyeEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarDyeable) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onDye", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}