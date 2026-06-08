package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BrewingStartEvent
import org.bukkit.event.inventory.BrewEvent
import org.bukkit.event.inventory.BrewingStandFuelEvent
import org.jetbrains.annotations.ApiStatus

interface BrewingStandRebarBlockHandler : VanillaInventoryRebarBlockHandler {
    fun onBrewingStandStart(event: BrewingStartEvent, priority: EventPriority) {}
    fun onFuelBrewingStand(event: BrewingStandFuelEvent, priority: EventPriority) {}
    fun onBrewingStandBrew(event: BrewEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onBrewingStandStart(event: BrewingStartEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is BrewingStandRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onBrewingStandStart", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onFuelBrewingStand(event: BrewingStandFuelEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is BrewingStandRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onFuelBrewingStand", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onBrewingStandBrew(event: BrewEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is BrewingStandRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onBrewingStandBrew", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}