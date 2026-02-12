package io.github.pylonmc.rebar.entity.base

import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.FireworkExplodeEvent

interface RebarFirework {
    fun onExplode(event: FireworkExplodeEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onFireworkExplode(event: FireworkExplodeEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarFirework) {
                try {
                    MultiHandler.handleEvent(rebarEntity, RebarFirework::class.java, "onExplode", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}