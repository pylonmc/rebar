package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.jetbrains.annotations.ApiStatus

/**
 * Called when an entity modified a block. This is called in many places - for example:
 * - When a wither breaks a block
 * - When an enderman takes/places a block
 * - When a sheep eats grass
 * - ...etc
 *
 * May be called alongside other events, such as [org.bukkit.event.entity.EntityBreakDoorEvent]
 *
 * @see EntityChangeBlockEvent
 */
interface RebarEntityChangedBlock {
    fun onEntityChanged(event: EntityChangeBlockEvent, priority: EventPriority)

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onEntityChangeBlock(event: EntityChangeBlockEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is RebarEntityChangedBlock) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onEntityChanged", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}