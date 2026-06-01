package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.papermc.paper.event.block.BlockFailedDispenseEvent
import io.papermc.paper.event.block.BlockPreDispenseEvent
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockDispenseArmorEvent
import org.bukkit.event.block.BlockDispenseEvent
import org.bukkit.event.block.BlockShearEntityEvent
import org.jetbrains.annotations.ApiStatus

interface DispenserRebarBlockHandler : VanillaInventoryRebarBlockHandler {
    fun onDispenseArmor(event: BlockDispenseArmorEvent, priority: EventPriority) {}
    fun onDispenseItem(event: BlockDispenseEvent, priority: EventPriority) {}
    fun onShearSheep(event: BlockShearEntityEvent, priority: EventPriority) {}
    fun onPreDispense(event: BlockPreDispenseEvent, priority: EventPriority) {}
    fun onDispenseFail(event: BlockFailedDispenseEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onDispenseArmor(event: BlockDispenseArmorEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is DispenserRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onDispenseArmor", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onDispenseItem(event: BlockDispenseEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is DispenserRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onDispenseItem", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onShearSheep(event: BlockShearEntityEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is DispenserRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onShearSheep", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onPreDispense(event: BlockPreDispenseEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is DispenserRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onPreDispense", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onDispenseFail(event: BlockFailedDispenseEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is DispenserRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onDispenseFail", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}