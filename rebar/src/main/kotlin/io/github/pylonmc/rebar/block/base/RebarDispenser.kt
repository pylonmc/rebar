package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.papermc.paper.event.block.BlockFailedDispenseEvent
import io.papermc.paper.event.block.BlockPreDispenseEvent
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockDispenseArmorEvent
import org.bukkit.event.block.BlockDispenseEvent
import org.bukkit.event.block.BlockDispenseLootEvent
import org.bukkit.event.block.BlockShearEntityEvent

interface RebarDispenser {
    fun onDispenseArmor(event: BlockDispenseArmorEvent, priority: EventPriority) {}
    fun onDispenseItem(event: BlockDispenseEvent, priority: EventPriority) {}
    fun onDispenseLoot(event: BlockDispenseLootEvent, priority: EventPriority) {}
    fun onShearSheep(event: BlockShearEntityEvent, priority: EventPriority) {}
    fun onPreDispense(event: BlockPreDispenseEvent, priority: EventPriority) {}
    fun onFailDispense(event: BlockFailedDispenseEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onDispenseArmor(event: BlockDispenseArmorEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarDispenser) {
                try {
                    MultiHandler.handleEvent(rebarBlock, "onDispenseArmor", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onDispenseItem(event: BlockDispenseEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarDispenser) {
                try {
                    MultiHandler.handleEvent(rebarBlock, "onDispenseItem", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onDispenseLoot(event: BlockDispenseLootEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarDispenser) {
                try {
                    MultiHandler.handleEvent(rebarBlock, "onDispenseLoot", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onShearSheep(event: BlockShearEntityEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarDispenser) {
                try {
                    MultiHandler.handleEvent(rebarBlock, "onShearSheep", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onPreDispense(event: BlockPreDispenseEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarDispenser) {
                try {
                    MultiHandler.handleEvent(rebarBlock, "onPreDispense", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onFailDispense(event: BlockFailedDispenseEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarDispenser) {
                try {
                    MultiHandler.handleEvent(rebarBlock, "onFailDispense", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}