package io.github.pylonmc.rebar.block.base

import com.destroystokyo.paper.event.player.PlayerSetSpawnEvent
import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.papermc.paper.event.player.PlayerBedFailEnterEvent
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerBedLeaveEvent
import org.jetbrains.annotations.ApiStatus

interface RebarBed {
    fun onFailEnterBed(event: PlayerBedFailEnterEvent, priority: EventPriority) {}
    fun onEnterBed(event: PlayerBedEnterEvent, priority: EventPriority) {}
    fun onLeaveBed(event: PlayerBedLeaveEvent, priority: EventPriority) {}
    fun onSetSpawn(event: PlayerSetSpawnEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onFailEnterBed(event: PlayerBedFailEnterEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.bed)
            if (rebarBlock is RebarBed) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onFailEnterBed", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onEnterBed(event: PlayerBedEnterEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.bed)
            if (rebarBlock is RebarBed) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onEnterBed", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onLeaveBed(event: PlayerBedLeaveEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.bed)
            if (rebarBlock is RebarBed) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onLeaveBed", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onSetSpawn(event: PlayerSetSpawnEvent, priority: EventPriority) {
            if (event.cause != PlayerSetSpawnEvent.Cause.BED) return

            val rebarBlock = BlockStorage.get(event.location)
            if (rebarBlock is RebarBed) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onSetSpawn", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}