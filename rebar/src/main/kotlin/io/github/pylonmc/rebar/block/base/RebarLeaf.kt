package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.LeavesDecayEvent

interface RebarLeaf {
    fun onDecayNaturally(event: LeavesDecayEvent, priority: EventPriority)

    companion object : MultiListener {
        @UniversalHandler
        private fun onLeafDecay(event: LeavesDecayEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarLeaf) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onDecayNaturally", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}