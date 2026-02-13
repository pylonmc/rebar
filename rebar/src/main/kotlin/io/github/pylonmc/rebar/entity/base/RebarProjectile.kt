package io.github.pylonmc.rebar.entity.base

import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.ProjectileHitEvent

interface RebarProjectile {
    fun onHit(event: ProjectileHitEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onProjectileHit(event: ProjectileHitEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarProjectile) {
                try {
                    MultiHandler.handleEvent(rebarEntity, "onHit", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}