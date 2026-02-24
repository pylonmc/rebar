package io.github.pylonmc.rebar.block.base

import com.destroystokyo.paper.event.block.BeaconEffectEvent
import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.papermc.paper.event.block.BeaconActivatedEvent
import io.papermc.paper.event.block.BeaconDeactivatedEvent
import io.papermc.paper.event.player.PlayerChangeBeaconEffectEvent
import org.bukkit.event.EventPriority

@Suppress("unused")
interface RebarBeacon {
    fun onActivated(event: BeaconActivatedEvent, priority: EventPriority) {}
    fun onDeactivated(event: BeaconDeactivatedEvent, priority: EventPriority) {}
    fun onEffectChange(event: PlayerChangeBeaconEffectEvent, priority: EventPriority) {}
    fun onEffectApply(event: BeaconEffectEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onBeaconActivate(event: BeaconActivatedEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarBeacon) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onActivated", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onBeaconDeactivate(event: BeaconDeactivatedEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarBeacon) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onDeactivated", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onBeaconChangeEffect(event: PlayerChangeBeaconEffectEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.beacon)
            if (rebarBlock is RebarBeacon) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onEffectChange", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onBeaconEffectApply(event: BeaconEffectEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarBeacon) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onEffectApply", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}