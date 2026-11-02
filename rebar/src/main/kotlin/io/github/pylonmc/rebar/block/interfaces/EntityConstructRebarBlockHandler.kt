package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.block.context.BlockBreakContext
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.util.position.BlockPosition
import io.github.pylonmc.rebar.util.rebarKey
import io.papermc.paper.event.entity.EntityConstructEvent
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntitySpawnEvent
import org.jetbrains.annotations.ApiStatus

/**
 * Represents and handles a block that can be used like its vanilla equivalent to construct entities.
 * For ex: a copper block for a copper golem, or a snow block for a snow golem.
 * If this interface is not implemented, the block will not be able to be used to construct entities like its vanilla counterpart.
 */
interface EntityConstructRebarBlockHandler {
    fun onEntityConstruct(event: EntityConstructEvent, priority: EventPriority) {}
    fun onEntitySpawn(event: EntitySpawnEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        private val rebarBlocksKey = rebarKey("rebar_construct_blocks")
        private val rebarBlocksType = RebarSerializers.LIST.listTypeFrom(RebarSerializers.BLOCK_POSITION)

        @UniversalHandler
        private fun onEntityConstruct(event: EntityConstructEvent, priority: EventPriority) {
            val positions = mutableListOf<BlockPosition>()
            for (block in event.blocks) {
                val rebarBlock = BlockStorage.get(block)
                if (rebarBlock is EntityConstructRebarBlockHandler) {
                    positions.add(BlockPosition(block))
                    try {
                        MultiHandlers.handleEvent(rebarBlock, "onEntityConstruct", event, priority)
                    } catch (e: Exception) {
                        BlockListener.logEventHandleErr(event, e, rebarBlock)
                    }
                }
            }

            if (positions.isNotEmpty()) {
                event.entity.persistentDataContainer.set(rebarBlocksKey, rebarBlocksType, positions)
            }
        }

        @UniversalHandler
        private fun onEntitySpawn(event: EntitySpawnEvent, priority: EventPriority) {
            val positions = event.entity.persistentDataContainer.get(rebarBlocksKey, rebarBlocksType) ?: return
            if (positions.isEmpty()) return
            for (position in positions) {
                val block = position.block
                val rebarBlock = BlockStorage.get(block)
                if (rebarBlock is EntityConstructRebarBlockHandler) {
                    try {
                        MultiHandlers.handleEvent(rebarBlock, "onEntitySpawn", event, priority)
                    } catch (e: Exception) {
                        BlockListener.logEventHandleErr(event, e, rebarBlock)
                    }
                }
            }

            if (priority == EventPriority.MONITOR && !event.isCancelled) {
                event.entity.persistentDataContainer.remove(rebarBlocksKey)
                for (position in positions) {
                    val block = position.block
                    val rebarBlock = BlockStorage.get(block)
                    if (rebarBlock is EntityConstructRebarBlockHandler) {
                        BlockStorage.breakBlock(rebarBlock, BlockBreakContext.EntityConstructed(block, event.entity))
                    }
                }
            }
        }
    }
}