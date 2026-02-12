package io.github.pylonmc.rebar.entity.base

import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityUnleashEvent
import org.bukkit.event.entity.PlayerLeashEntityEvent

interface RebarLeashable {
    fun onLeash(event: PlayerLeashEntityEvent, priority: EventPriority) {}
    fun onUnleash(event: EntityUnleashEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onLeash(event: PlayerLeashEntityEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarLeashable) {
                try {
                    MultiHandler.handleEvent(rebarEntity, "onLeash", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onUnleash(event: EntityUnleashEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarLeashable) {
                try {
                    MultiHandler.handleEvent(rebarEntity, "onUnleash", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}