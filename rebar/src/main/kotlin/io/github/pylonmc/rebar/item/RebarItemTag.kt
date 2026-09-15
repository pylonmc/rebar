package io.github.pylonmc.rebar.item

import io.github.pylonmc.rebar.block.RebarBlockTag
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType

/**
 * A tag that can contain both Vanilla and Rebar item types.
 *
 * For blocks, see [RebarBlockTag].
 */
class RebarItemTag(private val key: NamespacedKey, items: Set<ItemTypeWrapper>) : Tag<ItemTypeWrapper> {

    constructor(key: NamespacedKey, vararg materials: Material) : this(key, materials.map { ItemTypeWrapper(it) }.toSet())

    private val items = items.toMutableSet()

    fun add(wrapper: ItemTypeWrapper) {
        items.add(wrapper)
    }

    fun add(material: Material) = add(ItemTypeWrapper.Vanilla(material))

    fun add(schema: RebarItemSchema) = add(ItemTypeWrapper.Rebar(schema))

    fun add(key: NamespacedKey) = add(ItemTypeWrapper(key))

    fun add(item: ItemStack) = add(ItemTypeWrapper(item))

    override fun isTagged(item: ItemTypeWrapper): Boolean = item in items
    fun isTagged(material: Material): Boolean = ItemTypeWrapper(material) in items
    fun isTagged(itemType: ItemType): Boolean = ItemTypeWrapper(itemType) in items
    fun isTagged(schema: RebarItemSchema): Boolean = ItemTypeWrapper(schema) in items
    fun isTagged(key: NamespacedKey): Boolean = ItemTypeWrapper(key) in items
    override fun getValues(): Set<ItemTypeWrapper> = items.toSet()
    override fun getKey(): NamespacedKey = key

    @Suppress("unused")
    companion object {
        // @formatter:off
        /**
         * `rebar:rocks`:
         * - [`#minecraft:base_stone_overworld`](https://minecraft.wiki/w/Block_tag_(Java_Edition)#base_stone_overworld)
         * - `minecraft:cobblestone`
         * - `minecraft:cobbled_deepslate`
         * - `minecraft:blackstone`
         */
        @JvmField val ROCKS = RebarItemTag(
            rebarKey("rocks"),
            *Tag.BASE_STONE_OVERWORLD.values.toTypedArray(),
            Material.COBBLESTONE,
            Material.COBBLED_DEEPSLATE,
            Material.BLACKSTONE
        ).also { RebarRegistry.ITEM_TAGS.register(it) }
        // @formatter:on
    }
}