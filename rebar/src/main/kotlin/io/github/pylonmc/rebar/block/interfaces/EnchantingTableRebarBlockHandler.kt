package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.enchantment.PrepareItemEnchantEvent
import org.jetbrains.annotations.ApiStatus

interface EnchantingTableRebarBlockHandler {
    fun onPrepareEnchant(event: PrepareItemEnchantEvent, priority: EventPriority) {}
    fun onEnchantItem(event: EnchantItemEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onPrepareEnchant(event: PrepareItemEnchantEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.enchantBlock)
            if (rebarBlock is EnchantingTableRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onPrepareEnchant", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onEnchantItem(event: EnchantItemEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.enchantBlock)
            if (rebarBlock is EnchantingTableRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onEnchantItem", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}