package io.github.pylonmc.rebar.entity.base

import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.event.RebarEntityDeathEvent
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority

interface RebarDeathEntity {

    /**
     * Called when any entity is removed for any reason (except chunk unloading)
     */
    fun onDeath(event: RebarEntityDeathEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onDeath(event: RebarEntityDeathEvent, priority: EventPriority) {
            if (event.rebarEntity is RebarDeathEntity) {
                try {
                    MultiHandler.handleEvent(event.rebarEntity, RebarDeathEntity::class.java, "onDeath", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, event.rebarEntity)
                }
            }
        }
    }
}