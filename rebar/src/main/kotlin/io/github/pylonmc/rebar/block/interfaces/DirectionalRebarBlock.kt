package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.event.RebarBlockDeserializeEvent
import io.github.pylonmc.rebar.event.RebarBlockSerializeEvent
import io.github.pylonmc.rebar.event.RebarBlockUnloadEvent
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.Keyed
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.jetbrains.annotations.ApiStatus
import java.util.IdentityHashMap

/**
 * Represents a block that has a specific facing direction.
 *
 * Internally only used for rotating [RebarBlock.blockTextureEntity]s.
 */
interface DirectionalRebarBlock : Keyed {

    var facing: BlockFace
        get() = directionalBlocks[this] ?: error("No direction was set for block $key")
        set(value) {
            directionalBlocks[this] = value
        }

    @ApiStatus.Internal
    companion object : Listener {
        private val directionalBlockKey = rebarKey("directional_block")

        private val directionalBlocks = IdentityHashMap<DirectionalRebarBlock, BlockFace>()

        @EventHandler
        private fun onDeserialize(event: RebarBlockDeserializeEvent) {
            val block = event.rebarBlock
            if (block is DirectionalRebarBlock) {
                event.pdc.get(directionalBlockKey, RebarSerializers.BLOCK_FACE)?.let { directionalBlocks[block] = it }
            }
        }

        @EventHandler
        private fun onSerialize(event: RebarBlockSerializeEvent) {
            val block = event.rebarBlock
            if (block is DirectionalRebarBlock) {
                event.pdc.set(directionalBlockKey, RebarSerializers.BLOCK_FACE, directionalBlocks[block] ?: return)
            }
        }

        @EventHandler
        private fun onUnload(event: RebarBlockUnloadEvent) {
            val block = event.rebarBlock
            if (block is DirectionalRebarBlock) {
                directionalBlocks.remove(block)
            }
        }
    }
}