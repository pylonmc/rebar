package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.enchantment.PrepareItemEnchantEvent

interface RebarEnchantingTable {
    fun onPrepareEnchant(event: PrepareItemEnchantEvent, priority: EventPriority) {}
    fun onEnchant(event: EnchantItemEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onPreEnchant(event: PrepareItemEnchantEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.enchantBlock)
            if (rebarBlock is RebarEnchantingTable) {
                try {
                    MultiHandler.handleEvent(rebarBlock, "onPrepareEnchant", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onEnchant(event: EnchantItemEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.enchantBlock)
            if (rebarBlock is RebarEnchantingTable) {
                try {
                    MultiHandler.handleEvent(rebarBlock, "onEnchant", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}