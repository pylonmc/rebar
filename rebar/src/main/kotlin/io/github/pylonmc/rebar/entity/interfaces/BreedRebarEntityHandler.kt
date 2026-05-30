package io.github.pylonmc.rebar.entity.interfaces

import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityBreedEvent
import org.bukkit.event.entity.EntityEnterLoveModeEvent
import org.jetbrains.annotations.ApiStatus

interface BreedRebarEntityHandler {
    fun onBreed(event: EntityBreedEvent, priority: EventPriority) {}
    fun onEnterLoveMode(event: EntityEnterLoveModeEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onBreed(event: EntityBreedEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is BreedRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onBreed", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onEnterLoveMode(event: EntityEnterLoveModeEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is BreedRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onEnterLoveMode", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}