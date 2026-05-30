package io.github.pylonmc.rebar.entity.interfaces

import com.destroystokyo.paper.event.entity.EnderDragonFlameEvent
import com.destroystokyo.paper.event.entity.EnderDragonShootFireballEvent
import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EnderDragonChangePhaseEvent
import org.jetbrains.annotations.ApiStatus

interface EnderDragonRebarEntityHandler {
    fun onEnderDragonChangePhase(event: EnderDragonChangePhaseEvent, priority: EventPriority) {}
    fun onEnderDragonFlame(event: EnderDragonFlameEvent, priority: EventPriority) {}
    fun onEnderDragonShootFireball(event: EnderDragonShootFireballEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onEnderDragonChangePhase(event: EnderDragonChangePhaseEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is EnderDragonRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onEnderDragonChangePhase", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onEnderDragonFlame(event: EnderDragonFlameEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is EnderDragonRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onEnderDragonFlame", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onEnderDragonShootFireball(event: EnderDragonShootFireballEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is EnderDragonRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onEnderDragonShootFireball", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}