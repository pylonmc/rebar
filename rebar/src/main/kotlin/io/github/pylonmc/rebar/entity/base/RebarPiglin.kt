package io.github.pylonmc.rebar.entity.base

import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.PiglinBarterEvent

interface RebarPiglin {
    fun onBarter(event: PiglinBarterEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onBarter(event: PiglinBarterEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarPiglin) {
                try {
                    MultiHandler.handleEvent(rebarEntity, "onBarter", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}