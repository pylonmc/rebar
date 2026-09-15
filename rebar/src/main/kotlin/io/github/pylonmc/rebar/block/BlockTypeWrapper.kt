package io.github.pylonmc.rebar.block

import io.github.pylonmc.rebar.block.context.BlockCreateContext
import io.github.pylonmc.rebar.item.ItemTypeWrapper
import io.github.pylonmc.rebar.registry.RebarRegistry
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.BlockType
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Allows the representation of both vanilla and Rebar blocks in a unified way.
 * @see ItemTypeWrapper
 */
sealed interface BlockTypeWrapper : Keyed {

    fun matches(block: Block?): Boolean

    fun createItemStack() = createItemStack(1)

    fun createItemStack(count: Int): ItemStack

    /**
     * Places this block type at [block], using [player] as the context for placement if applicable.
     * @return true if the block was placed successfully, false otherwise.
     *
     * Note: This method does not care what [block] is currently
     */
    fun placeBlock(block: Block?, player: Player?): Boolean

    /**
     * The vanilla variant of [BlockTypeWrapper].
     */
    @JvmRecord
    data class Vanilla(val blockData: BlockData) : BlockTypeWrapper {
        override fun matches(block: Block?) = block != null && !BlockStorage.isRebarBlock(block) && blockData.matches(block.blockData)
        override fun createItemStack(count: Int) = ItemStack.of(blockData.material, count)
        override fun placeBlock(block: Block?, player: Player?): Boolean {
            // TODO: use player place api when its introduced
            if (block == null) return false
            block.blockData = blockData
            return true
        }
        override fun getKey() = blockData.material.key
    }

    /**
     * The Rebar variant of [BlockTypeWrapper].
     */
    @JvmRecord
    data class Rebar(val blockSchema: RebarBlockSchema) : BlockTypeWrapper {
        override fun matches(block: Block?) = block != null && BlockStorage.get(block)?.schema == blockSchema
        override fun createItemStack(count: Int) = blockSchema.defaultItem?.getItemStack(count) ?: throw IllegalArgumentException("Block schema ${blockSchema.key} does not have an item representation.")
        // TODO: use player place api when its introduced
        override fun placeBlock(block: Block?, player: Player?) =
            block != null && BlockStorage.placeBlock(block, blockSchema.key, BlockCreateContext.Default(player, block)) != null
        override fun getKey() = blockSchema.key
    }

    companion object {
        @JvmStatic
        val AIR = BlockTypeWrapper(Material.AIR)

        @JvmStatic
        @JvmName("of")
        operator fun invoke(block: Block): BlockTypeWrapper {
            val schema = BlockStorage.get(block)?.schema
            return if (schema != null) Rebar(schema) else BlockTypeWrapper(block.blockData)
        }

        @JvmStatic
        @JvmName("of")
        operator fun invoke(blockSchema: RebarBlockSchema): BlockTypeWrapper {
            return Rebar(blockSchema)
        }

        @JvmStatic
        @JvmName("of")
        operator fun invoke(material: Material): BlockTypeWrapper {
            return Vanilla(material.createBlockData())
        }

        @JvmStatic
        @JvmName("of")
        operator fun invoke(blockType: BlockType): BlockTypeWrapper {
            return Vanilla(blockType.createBlockData())
        }

        @JvmStatic
        @JvmName("of")
        operator fun invoke(blockData: BlockData): BlockTypeWrapper {
            return Vanilla(blockData)
        }

        @JvmStatic
        @JvmName("of")
        operator fun invoke(key: NamespacedKey): BlockTypeWrapper {
            return RebarRegistry.BLOCKS[key]?.let(::Rebar)
                ?: Registry.BLOCK.get(key)?.let { BlockTypeWrapper(it.createBlockData()) }
                ?: throw IllegalArgumentException("No block found for key $key")
        }

        @JvmStatic
        @JvmName("materialTagToBlockTypeTag")
        fun Tag<Material>.toBlockTypeTag(): Tag<BlockTypeWrapper> {
            val blockWrappers = values.mapTo(mutableSetOf()) { BlockTypeWrapper(it) }
            return RebarBlockTag(key, blockWrappers)
        }
    }
}