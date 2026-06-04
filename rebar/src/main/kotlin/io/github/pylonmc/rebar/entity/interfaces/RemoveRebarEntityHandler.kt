package io.github.pylonmc.rebar.entity.interfaces

import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityRemoveEvent
import org.jetbrains.annotations.ApiStatus

interface RemoveRebarEntityHandler {

    /**
     * Called when an entity is removed for any reason except unloading (use [UnloadRebarEntityHandler] for that)
     */
    fun onRemoved(event: EntityRemoveEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onRemoved(event: EntityRemoveEvent, priority: EventPriority) {
            if (event.cause == EntityRemoveEvent.Cause.UNLOAD) return
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RemoveRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onRemoved", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}