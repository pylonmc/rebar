package io.github.pylonmc.rebar.entity.base

import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityRegainHealthEvent

interface RebarDamageableEntity {
    fun onDamage(event: EntityDamageEvent, priority: EventPriority) {}
    fun onRegainHealth(event: EntityRegainHealthEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onDamage(event: EntityDamageEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarDamageableEntity) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onDamage", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onRegainHealth(event: EntityRegainHealthEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarDamageableEntity) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onRegainHealth", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}