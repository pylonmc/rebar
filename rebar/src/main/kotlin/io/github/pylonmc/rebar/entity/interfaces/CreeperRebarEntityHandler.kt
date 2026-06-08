package io.github.pylonmc.rebar.entity.interfaces

import com.destroystokyo.paper.event.entity.CreeperIgniteEvent
import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.CreeperPowerEvent
import org.jetbrains.annotations.ApiStatus

interface CreeperRebarEntityHandler : ExplosiveRebarEntityHandler {
    fun onCreeperIgnite(event: CreeperIgniteEvent, priority: EventPriority) {}
    fun onCreeperPower(event: CreeperPowerEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onCreeperIgnite(event: CreeperIgniteEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is CreeperRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onCreeperIgnite", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onCreeperPower(event: CreeperPowerEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is CreeperRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onCreeperPower", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}