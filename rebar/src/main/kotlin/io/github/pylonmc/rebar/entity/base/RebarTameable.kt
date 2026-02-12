package io.github.pylonmc.rebar.entity.base

import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.papermc.paper.event.entity.TameableDeathMessageEvent
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityTameEvent

interface RebarTameable {
    fun onTamed(event: EntityTameEvent, priority: EventPriority) {}
    fun onDeath(event: TameableDeathMessageEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onTamed(event: EntityTameEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarTameable) {
                try {
                    MultiHandler.handleEvent(rebarEntity, RebarTameable::class.java, "onTamed", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onTameableDeath(event: TameableDeathMessageEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarTameable) {
                try {
                    MultiHandler.handleEvent(rebarEntity, RebarTameable::class.java, "onDeath", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}