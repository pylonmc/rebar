package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.item.ItemTypeWrapper
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

object ItemStackConfigAdapter : ConfigAdapter<ItemStack> {

    override val type = ItemStack::class.java

    override fun convert(value: Any): ItemStack {
        return when (value) {
            is Pair<*, *> -> {
                val itemKey = ConfigAdapter.STRING.convert(value.first!!)
                val amount = ConfigAdapter.INTEGER.convert(value.second!!)
                convert(itemKey).asQuantity(amount)
            }

            is ConfigurationSection, is Map<*, *> -> convert(MapConfigAdapter.STRING_TO_ANY.convert(value).toList().single())
            is String -> ItemTypeWrapper(
                NamespacedKey.fromString(value) ?: throw IllegalArgumentException("Could not find item '$value'")
            ).createItemStack()
            else -> throw IllegalArgumentException("Cannot convert $value to ItemStack")
        }
    }
}