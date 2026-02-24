package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.block.Container
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.inventory.BlockInventoryHolder

/**
 * Represents blocks which can naturally store items such as chests and hoppers.
 */
interface RebarVanillaContainerBlock {
    fun onInventoryOpen(event: InventoryOpenEvent, priority: EventPriority) {}
    fun onItemMoveTo(event: InventoryMoveItemEvent, priority: EventPriority) {}
    fun onItemMoveFrom(event: InventoryMoveItemEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onInventoryOpen(event: InventoryOpenEvent, priority: EventPriority) {
            val holder = event.inventory.holder
            if (holder is Container) {
                val rebarBlock = BlockStorage.get(holder.block)
                if (rebarBlock is RebarVanillaContainerBlock) {
                    try {
                        MultiHandlers.handleEvent(rebarBlock, "onInventoryOpen", event, priority)
                    } catch (e: Exception) {
                        BlockListener.logEventHandleErr(event, e, rebarBlock)
                    }
                }
            }
        }

        @UniversalHandler
        private fun onItemMove(event: InventoryMoveItemEvent, priority: EventPriority) {
            val sourceHolder = event.source.holder
            if (sourceHolder is BlockInventoryHolder) {
                val sourceBlock = BlockStorage.get(sourceHolder.block)
                if (sourceBlock is RebarVanillaContainerBlock) {
                    try {
                        MultiHandlers.handleEvent(sourceBlock, "onItemMoveFrom", event, priority)
                    } catch (e: Exception) {
                        BlockListener.logEventHandleErr(event, e, sourceBlock)
                    }
                }
            }

            val destHolder = event.destination.holder
            if (destHolder is BlockInventoryHolder) {
                val destBlock = BlockStorage.get(destHolder.block)
                if (destBlock is RebarVanillaContainerBlock) {
                    try {
                        MultiHandlers.handleEvent(destBlock, "onItemMoveTo", event, priority)
                    } catch (e: Exception) {
                        BlockListener.logEventHandleErr(event, e, destBlock)
                    }
                }
            }
        }
    }
}