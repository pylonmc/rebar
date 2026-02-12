package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.papermc.paper.event.player.PlayerInsertLecternBookEvent
import io.papermc.paper.event.player.PlayerLecternPageChangeEvent
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerTakeLecternBookEvent

interface RebarLectern {
    fun onInsertBook(event: PlayerInsertLecternBookEvent, priority: EventPriority) {}
    fun onRemoveBook(event: PlayerTakeLecternBookEvent, priority: EventPriority) {}
    fun onChangePage(event: PlayerLecternPageChangeEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onLecternInsertBook(event: PlayerInsertLecternBookEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarLectern) {
                try {
                    MultiHandler.handleEvent(rebarBlock, "onInsertBook", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onLecternRemoveBook(event: PlayerTakeLecternBookEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.lectern.block)
            if (rebarBlock is RebarLectern) {
                try {
                    MultiHandler.handleEvent(rebarBlock, "onRemoveBook", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }

        @UniversalHandler
        private fun onLecternChangePage(event: PlayerLecternPageChangeEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.lectern.block)
            if (rebarBlock is RebarLectern) {
                try {
                    MultiHandler.handleEvent(rebarBlock, "onChangePage", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}