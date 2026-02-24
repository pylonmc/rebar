package io.github.pylonmc.rebar.entity.base

import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.event.RebarEntityUnloadEvent
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority

interface RebarUnloadEntity {
    fun onUnload(event: RebarEntityUnloadEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onUnload(event: RebarEntityUnloadEvent, priority: EventPriority) {
            if (event.rebarEntity is RebarUnloadEntity) {
                try {
                    MultiHandlers.handleEvent(event.rebarEntity, "onUnload", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, event.rebarEntity)
                }
            }
        }
    }
}