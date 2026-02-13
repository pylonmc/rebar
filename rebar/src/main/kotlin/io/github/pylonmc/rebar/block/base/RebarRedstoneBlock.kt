package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockRedstoneEvent

interface RebarRedstoneBlock {
    fun onCurrentChange(event: BlockRedstoneEvent, priority: EventPriority)

    companion object : MultiListener {
        @UniversalHandler
        private fun onRedstoneCurrentChange(event: BlockRedstoneEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarRedstoneBlock) {
                try {
                    MultiHandler.handleEvent(rebarBlock, "onCurrentChange", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}