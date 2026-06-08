package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.nms.NmsAccessor
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

object ItemStackConfigAdapter : ConfigAdapter<ItemStack> {

    override val type = ItemStack::class.java

    override fun convert(key: String?, value: Any): ItemStack {
        return when (value) {
            is Pair<*, *> -> {
                val itemKey = ConfigAdapter.STRING.convert(key, value.first!!)
                val amount = ConfigAdapter.INTEGER.convert(key, value.second!!)
                convert(key, itemKey).apply { this.amount = amount }
            }

            is ConfigurationSection, is Map<*, *> -> convert(key, MapConfigAdapter.STRING_TO_ANY.convert(key, value).toList().single())
            is String -> NmsAccessor.instance.createItemStack(value)
            else -> throw IllegalArgumentException("Cannot convert $value to ItemStack")
        }
    }
}