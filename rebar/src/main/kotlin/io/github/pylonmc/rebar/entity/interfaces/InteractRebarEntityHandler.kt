package io.github.pylonmc.rebar.entity.interfaces

import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.jetbrains.annotations.ApiStatus

interface InteractRebarEntityHandler {

    /**
     * This may be called for both hands, so make sure you check which hand is used.
     */
    fun onInteractedWith(event: PlayerInteractEntityEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onInteractedWith(event: PlayerInteractEntityEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.rightClicked)
            if (rebarEntity is InteractRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onInteractedWith", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}