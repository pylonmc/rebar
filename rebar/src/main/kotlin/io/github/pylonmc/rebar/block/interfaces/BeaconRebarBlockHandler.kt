package io.github.pylonmc.rebar.block.interfaces

import com.destroystokyo.paper.event.block.BeaconEffectEvent
import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.util.isChunkLoaded
import io.papermc.paper.event.block.BeaconActivatedEvent
import io.papermc.paper.event.block.BeaconDeactivatedEvent
import io.papermc.paper.event.player.PlayerChangeBeaconEffectEvent
import org.bukkit.event.EventPriority
import org.jetbrains.annotations.ApiStatus

@Suppress("unused")
interface BeaconRebarBlockHandler {
    fun onBeaconActivate(event: BeaconActivatedEvent, priority: EventPriority) {}
    fun onBeaconDeactivate(event: BeaconDeactivatedEvent, priority: EventPriority) {}
    fun onBeaconEffectChange(event: PlayerChangeBeaconEffectEvent, priority: EventPriority) {}
    fun onBeaconEffectApply(event: BeaconEffectEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onBeaconActivate(event: BeaconActivatedEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is BeaconRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onBeaconActivate", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onBeaconDeactivate(event: BeaconDeactivatedEvent, priority: EventPriority) {
            // https://github.com/PaperMC/Paper/issues/8947#issuecomment-1485388179
            if (!event.block.isChunkLoaded) {
                return
            }

            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is BeaconRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onBeaconDeactivate", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onBeaconEffectChange(event: PlayerChangeBeaconEffectEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.beacon)
            if (rebarBlock is BeaconRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onBeaconEffectChange", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onBeaconEffectApply(event: BeaconEffectEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is BeaconRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onBeaconEffectApply", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}