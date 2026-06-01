package io.github.pylonmc.rebar.entity.interfaces

import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDismountEvent
import org.bukkit.event.entity.EntityMountEvent
import org.jetbrains.annotations.ApiStatus

interface PassengerRebarEntityHandler {
    fun onMount(event: EntityMountEvent, priority: EventPriority) {}
    fun onDismount(event: EntityDismountEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onMount(event: EntityMountEvent, priority: EventPriority) {
            val passenger = EntityStorage.get(event.entity)
            if (passenger is PassengerRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(passenger, "onMount", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, passenger)
                }
            }
        }

        @UniversalHandler
        private fun onDismount(event: EntityDismountEvent, priority: EventPriority) {
            val passenger = EntityStorage.get(event.entity)
            if (passenger is PassengerRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(passenger, "onDismount", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, passenger)
                }
            }
        }
    }
}