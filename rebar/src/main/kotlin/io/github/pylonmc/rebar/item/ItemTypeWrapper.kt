package io.github.pylonmc.rebar.item

import io.github.pylonmc.rebar.registry.RebarRegistry
import io.papermc.paper.datacomponent.DataComponentType
import org.bukkit.*
import org.bukkit.inventory.ItemStack

/**
 * Allows the representation of both vanilla and Rebar items in a unified way.
 */
sealed interface ItemTypeWrapper : Keyed {

    fun createItemStack(): ItemStack
    fun matchesWithoutData(item: ItemStack, excludeTypes: Set<DataComponentType>, ignoreCount: Boolean = false): Boolean
    fun matchesWithoutData(item: ItemStack, excludeTypes: Set<DataComponentType>): Boolean = matchesWithoutData(item, excludeTypes, false)
    fun isEmpty(): Boolean

    /**
     * The vanilla variant of [ItemTypeWrapper].
     */
    @JvmRecord
    data class Vanilla(val material: Material) : ItemTypeWrapper {
        override fun createItemStack() = ItemStack(material)
        override fun matchesWithoutData(item: ItemStack, excludeTypes: Set<DataComponentType>, ignoreCount: Boolean) = createItemStack().matchesWithoutData(item, excludeTypes, ignoreCount)
        override fun getKey() = material.key
        override fun isEmpty() = material.isAir()
        override fun toString() = "ItemTypeWrapper.Vanilla(${material.key})"
    }

    /**
     * The Rebar variant of [ItemTypeWrapper].
     */
    @JvmRecord
    data class Rebar(val item: RebarItemSchema) : ItemTypeWrapper {
        override fun createItemStack() = item.getItemStack()
        override fun matchesWithoutData(item: ItemStack, excludeTypes: Set<DataComponentType>, ignoreCount: Boolean) = this.item.matchesWithoutData(item, excludeTypes, ignoreCount)
        override fun getKey() = item.key
        override fun isEmpty() = false
        override fun toString() = "ItemTypeWrapper.Rebar(${item.key})"
    }

    companion object {
        @JvmStatic
        @JvmName("of")
        operator fun invoke(stack: ItemStack): ItemTypeWrapper {
            val item = RebarItem.fromStack(stack)
            return if (item != null) Rebar(item.schema) else Vanilla(stack.type)
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