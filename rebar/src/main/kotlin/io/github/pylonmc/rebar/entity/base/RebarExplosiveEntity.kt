package io.github.pylonmc.rebar.entity.base

import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.ExplosionPrimeEvent

interface RebarExplosiveEntity {
    fun onPrime(event: ExplosionPrimeEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onPrime(event: ExplosionPrimeEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarExplosiveEntity) {
                try {
                    MultiHandler.handleEvent(rebarEntity, "onPrime", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}