package io.github.pylonmc.rebar.entity.base

import com.destroystokyo.paper.event.entity.EnderDragonFlameEvent
import com.destroystokyo.paper.event.entity.EnderDragonShootFireballEvent
import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EnderDragonChangePhaseEvent

interface RebarEnderDragon {
    fun onChangePhase(event: EnderDragonChangePhaseEvent, priority: EventPriority) {}
    fun onFlame(event: EnderDragonFlameEvent, priority: EventPriority) {}
    fun onShootFireball(event: EnderDragonShootFireballEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onDragonChangePhase(event: EnderDragonChangePhaseEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarEnderDragon) {
                try {
                    MultiHandler.handleEvent(rebarEntity, "onChangePhase", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onDragonFlame(event: EnderDragonFlameEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarEnderDragon) {
                try {
                    MultiHandler.handleEvent(rebarEntity, "onFlame", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onDragonShootFireball(event: EnderDragonShootFireballEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarEnderDragon) {
                try {
                    MultiHandler.handleEvent(rebarEntity, "onShootFireball", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}