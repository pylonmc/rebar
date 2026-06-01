package io.github.pylonmc.rebar.entity.interfaces

import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.PigZombieAngerEvent
import org.jetbrains.annotations.ApiStatus

interface ZombifiedPiglinRebarEntityHandler {
    fun onZombifiedPiglinAnger(event: PigZombieAngerEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onZombifiedPiglinAnger(event: PigZombieAngerEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is ZombifiedPiglinRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onZombifiedPiglinAnger", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}