package io.github.pylonmc.rebar.entity.base

import com.destroystokyo.paper.event.entity.EndermanAttackPlayerEvent
import com.destroystokyo.paper.event.entity.EndermanEscapeEvent
import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority

interface RebarEnderman {
    fun onAttackPlayer(event: EndermanAttackPlayerEvent, priority: EventPriority) {}
    fun onEscape(event: EndermanEscapeEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onEndermanAttackPlayer(event: EndermanAttackPlayerEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarEnderman) {
                try {
                    MultiHandler.handleEvent(rebarEntity, "onAttackPlayer", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onEndermanEscape(event: EndermanEscapeEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarEnderman) {
                try {
                    MultiHandler.handleEvent(rebarEntity, "onEscape", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}