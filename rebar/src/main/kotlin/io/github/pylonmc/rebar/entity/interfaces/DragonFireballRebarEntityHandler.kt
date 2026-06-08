package io.github.pylonmc.rebar.entity.interfaces

import com.destroystokyo.paper.event.entity.EnderDragonFireballHitEvent
import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.jetbrains.annotations.ApiStatus

interface DragonFireballRebarEntityHandler {
    fun onDragonFireballHit(event: EnderDragonFireballHitEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onDragonFireballHit(event: EnderDragonFireballHitEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is DragonFireballRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onDragonFireballHit", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}