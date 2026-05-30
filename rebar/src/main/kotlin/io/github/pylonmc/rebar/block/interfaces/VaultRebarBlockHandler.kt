package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.VaultDisplayItemEvent
import org.jetbrains.annotations.ApiStatus

interface VaultRebarBlockHandler : LootDispenserRebarBlockHandler {
    fun onVaultDisplayItem(event: VaultDisplayItemEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onVaultDisplayItem(event: VaultDisplayItemEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is VaultRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onVaultDisplayItem", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}