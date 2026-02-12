package io.github.pylonmc.rebar.entity.base

import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerInteractEntityEvent

interface RebarInteractEntity {

    /**
     * This may be called for both hands, so make sure you check which hand is used.
     */
    fun onInteract(event: PlayerInteractEntityEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onInteractEntity(event: PlayerInteractEntityEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.rightClicked)
            if (rebarEntity is RebarInteractEntity) {
                try {
                    MultiHandler.handleEvent(rebarEntity, RebarInteractEntity::class.java, "onInteract", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}