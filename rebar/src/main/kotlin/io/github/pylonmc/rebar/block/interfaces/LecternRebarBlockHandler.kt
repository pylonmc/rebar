package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.papermc.paper.event.player.PlayerInsertLecternBookEvent
import io.papermc.paper.event.player.PlayerLecternPageChangeEvent
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.player.PlayerTakeLecternBookEvent
import org.jetbrains.annotations.ApiStatus

interface LecternRebarBlockHandler : VanillaInventoryRebarBlockHandler {
    fun onLecternBookInsert(event: PlayerInsertLecternBookEvent, priority: EventPriority) {}
    fun onLecternBookRemove(event: PlayerTakeLecternBookEvent, priority: EventPriority) {}
    fun onLecternPageChange(event: PlayerLecternPageChangeEvent, priority: EventPriority) {}

    /**
     * Note: vanilla hoppers & hopper minecarts do not currently interact with lecterns
     */
    override fun onItemMoveTo(event: InventoryMoveItemEvent, priority: EventPriority) {}

    /**
     * Note: vanilla hoppers & hopper minecarts do not currently interact with lecterns
     */
    override fun onItemMoveFrom(event: InventoryMoveItemEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onLecternBookInsert(event: PlayerInsertLecternBookEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is LecternRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onLecternBookInsert", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onLecternBookRemove(event: PlayerTakeLecternBookEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.lectern.block)
            if (rebarBlock is LecternRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onLecternBookRemove", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onLecternPageChange(event: PlayerLecternPageChangeEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.lectern.block)
            if (rebarBlock is LecternRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onLecternPageChange", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}