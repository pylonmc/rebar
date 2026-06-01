package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockDispenseLootEvent

interface LootDispenserRebarBlockHandler {
    /**
     * Called when this block dispenses loot
     *
     * For example: A Trial Spawner or Vault Block
     */
    fun onDispenseLoot(event: BlockDispenseLootEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onDispenseLoot(event: BlockDispenseLootEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is DispenserRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onDispenseLoot", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}