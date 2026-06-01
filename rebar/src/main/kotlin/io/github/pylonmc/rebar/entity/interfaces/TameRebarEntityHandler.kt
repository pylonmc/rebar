package io.github.pylonmc.rebar.entity.interfaces

import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.papermc.paper.event.entity.TameableDeathMessageEvent
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityTameEvent
import org.jetbrains.annotations.ApiStatus

interface TameRebarEntityHandler {
    fun onTamed(event: EntityTameEvent, priority: EventPriority) {}
    fun onTamedDeathMessage(event: TameableDeathMessageEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onTamed(event: EntityTameEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is TameRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onTamed", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onTamedDeathMessage(event: TameableDeathMessageEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is TameRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onTamedDeathMessage", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}