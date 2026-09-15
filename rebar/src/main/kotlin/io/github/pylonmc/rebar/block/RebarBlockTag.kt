package io.github.pylonmc.rebar.block

import io.github.pylonmc.rebar.item.RebarItemTag
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.block.BlockType
import org.bukkit.block.data.BlockData

/**
 * A tag that can contain both Vanilla and Rebar block types.
 *
 * For items, see [RebarItemTag].
 */
class RebarBlockTag(private val key: NamespacedKey, blocks: Set<BlockTypeWrapper>) : Tag<BlockTypeWrapper> {
    constructor(key: NamespacedKey, vararg materials: Material) : this(key, materials.map { BlockTypeWrapper(it) }.toSet())

    private val blocks = blocks.toMutableSet()

    fun add(wrapper: BlockTypeWrapper) {
        blocks.add(wrapper)
    }

    fun add(material: Material) = add(BlockTypeWrapper(material))

    fun add(blockData: BlockData) = add(BlockTypeWrapper(blockData))

    fun add(schema: RebarBlockSchema) = add(BlockTypeWrapper(schema))

    fun add(key: NamespacedKey) = add(BlockTypeWrapper(key))

    override fun isTagged(block: BlockTypeWrapper): Boolean = block in blocks
    fun isTagged(material: Material): Boolean = BlockTypeWrapper(material) in blocks
    fun isTagged(blockType: BlockType): Boolean = BlockTypeWrapper(blockType) in blocks
    fun isTagged(blockData: BlockData): Boolean = BlockTypeWrapper(blockData) in blocks
    fun isTagged(schema: RebarBlockSchema): Boolean = BlockTypeWrapper(schema) in blocks
    fun isTagged(key: NamespacedKey): Boolean = BlockTypeWrapper(key) in blocks
    override fun getValues(): Set<BlockTypeWrapper> = blocks.toSet()
    override fun getKey(): NamespacedKey = key
}