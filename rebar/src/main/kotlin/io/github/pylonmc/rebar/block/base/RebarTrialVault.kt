package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.VaultDisplayItemEvent

interface RebarTrialVault {
    fun onDisplayItem(event: VaultDisplayItemEvent, priority: EventPriority)

    companion object : MultiListener {
        @UniversalHandler
        private fun onVaultDisplayItem(event: VaultDisplayItemEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarTrialVault) {
                try {
                    MultiHandler.handleEvent(rebarBlock, "onDisplayItem", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}