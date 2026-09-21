package io.github.pylonmc.rebar.item

import io.github.pylonmc.rebar.block.BlockTypeWrapper
import io.github.pylonmc.rebar.registry.RebarRegistry
import org.bukkit.*
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType

/**
 * Allows the representation of both vanilla and Rebar items in a unified way.
 * @see BlockTypeWrapper
 */
sealed interface ItemTypeWrapper : Keyed {

    fun matches(itemStack: ItemStack?): Boolean

    fun createItemStack() = createItemStack(1)

    fun createItemStack(count: Int): ItemStack

    /**
     * The vanilla variant of [ItemTypeWrapper].
     */
    @JvmRecord
    data class Vanilla(val material: Material) : ItemTypeWrapper {
        override fun matches(itemStack: ItemStack?) = itemStack?.type == material && !RebarItem.isRebarItem(itemStack)
        override fun createItemStack(count: Int) = ItemStack.of(material, count)
        override fun getKey() = material.key
    }

    /**
     * The Rebar variant of [ItemTypeWrapper].
     */
    @JvmRecord
    data class Rebar(val item: RebarItemSchema) : ItemTypeWrapper {
        override fun matches(itemStack: ItemStack?) = RebarItem.isRebarItem(itemStack, item)
        override fun createItemStack(count: Int) = item.createNewItemStack(count)
        override fun getKey() = item.key
    }

    companion object {
        @JvmStatic
        val AIR = ItemTypeWrapper(Material.AIR)

        @JvmStatic
        @JvmName("of")
        operator fun invoke(stack: ItemStack): ItemTypeWrapper {
            val schema = RebarItemSchema.fromStack(stack)
            return if (schema != null) Rebar(schema) else Vanilla(stack.type)
        }

        @JvmStatic
        @JvmName("of")
        operator fun invoke(schema: RebarItemSchema): ItemTypeWrapper {
            return Rebar(schema)
        }

        @JvmStatic
        @JvmName("of")
        operator fun invoke(itemType: ItemType): ItemTypeWrapper {
            return ItemTypeWrapper(itemType.key)
        }

        @JvmStatic
        @JvmName("of")
        operator fun invoke(material: Material): ItemTypeWrapper {
            return Vanilla(material)
        }

        @JvmStatic
        @JvmName("of")
        operator fun invoke(key: NamespacedKey): ItemTypeWrapper {
            return RebarRegistry.ITEMS[key]?.let(::Rebar)
                ?: Registry.MATERIAL.get(key)?.let(::Vanilla)
                ?: throw IllegalArgumentException("No item found for key $key")
        }

        @JvmStatic
        @JvmName("materialTagToItemTypeTag")
        fun Tag<Material>.toItemTypeTag(): Tag<ItemTypeWrapper> {
            val itemWrappers = values.mapTo(mutableSetOf(), ItemTypeWrapper::Vanilla)
            return RebarItemTag(key, itemWrappers)
        }
    }
}