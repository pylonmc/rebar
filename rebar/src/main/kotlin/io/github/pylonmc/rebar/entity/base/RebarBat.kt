package io.github.pylonmc.rebar.entity.base

import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.BatToggleSleepEvent

interface RebarBat {
    fun onToggleSleep(event: BatToggleSleepEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onBatToggleSleep(event: BatToggleSleepEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarBat) {
                try {
                    MultiHandler.handleEvent(rebarEntity, RebarBat::class.java, "onToggleSleep", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}