package io.github.pylonmc.rebar.entity.interfaces

import com.destroystokyo.paper.event.entity.EndermanAttackPlayerEvent
import com.destroystokyo.paper.event.entity.EndermanEscapeEvent
import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.jetbrains.annotations.ApiStatus

interface EndermanRebarEntityHandler {
    fun onEndermanAttackPlayer(event: EndermanAttackPlayerEvent, priority: EventPriority) {}
    fun onEndermanEscape(event: EndermanEscapeEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onEndermanAttackPlayer(event: EndermanAttackPlayerEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is EndermanRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onEndermanAttackPlayer", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onEndermanEscape(event: EndermanEscapeEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is EndermanRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onEndermanEscape", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}