package io.github.pylonmc.rebar.entity.interfaces

import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.event.entity.EntityCombustByBlockEvent
import org.bukkit.event.entity.EntityCombustByEntityEvent
import org.jetbrains.annotations.ApiStatus

interface CombustRebarEntityHandler {
    /**
     * Called when this entity is set on fire
     *
     * Note: [event] could be [EntityCombustByBlockEvent] or [EntityCombustByEntityEvent]
     */
    fun onCombust(event: EntityCombustEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onCombust(event: EntityCombustEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is CombustRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onCombust", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}
