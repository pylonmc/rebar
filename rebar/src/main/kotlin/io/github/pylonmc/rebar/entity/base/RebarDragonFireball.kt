package io.github.pylonmc.rebar.entity.base

import com.destroystokyo.paper.event.entity.EnderDragonFireballHitEvent
import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority

interface RebarDragonFireball {
    fun onHit(event: EnderDragonFireballHitEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onFireballHit(event: EnderDragonFireballHitEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarDragonFireball) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onHit", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}