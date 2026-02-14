package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.item.ItemTypeWrapper
import io.github.pylonmc.rebar.recipe.FluidOrItem
import io.github.pylonmc.rebar.recipe.RecipeInput
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection

object RecipeInputConfigAdapter : ConfigAdapter<RecipeInput> {

    override val type = FluidOrItem::class.java

    override fun convert(value: Any): RecipeInput {
        runCatching { RecipeInputItemAdapter.convert(value) }.onSuccess { return it }
        runCatching { RecipeInputFluidAdapter.convert(value) }.onSuccess { return it }
        throw IllegalArgumentException("Cannot convert $value to recipe input")
    }
}

object RecipeInputItemAdapter : ConfigAdapter<RecipeInput.Item> {
    override val type = RecipeInput.Item::class.java

    override fun convert(value: Any): RecipeInput.Item {
        runCatching { ConfigAdapter.ITEM_STACK.convert(value) }.onSuccess { return RecipeInput.of(it) }
        return when (value) {
            is ConfigurationSection, is Map<*, *> -> convert(
                MapConfigAdapter.STRING_TO_ANY.convert(value).toList().single()
            )

            is Pair<*, *> -> {
                val tag = ConfigAdapter.ITEM_TAG.convert(value.first!!)
                val amount = ConfigAdapter.INTEGER.convert(value.second!!)
                RecipeInput.Item(tag, amount)
            }

            is String -> {
                if (value.startsWith("#")) {
                    RecipeInput.Item(ConfigAdapter.ITEM_TAG.convert(value), 1)
                } else {
                    val nsKey = NamespacedKey.fromString(value) ?: throw IllegalArgumentException("'$value' is not a namespaced key")
                    RecipeInput.Item(hashSetOf(ItemTypeWrapper.invoke(nsKey)), 1)
                }
            }
            else -> throw IllegalArgumentException("Cannot convert $value to item recipe input")
        }
    }
}

object RecipeInputFluidAdapter : ConfigAdapter<RecipeInput.Fluid> {
    override val type = RecipeInput.Fluid::class.java

    override fun convert(value: Any): RecipeInput.Fluid {
        return when (value) {
            is ConfigurationSection, is Map<*, *> -> convert(
                MapConfigAdapter.STRING_TO_ANY.convert(value).toList().single()
            )

            is Pair<*, *> -> {
                val fluid = ConfigAdapter.REBAR_FLUID.convert(value.first!!)
                val amount = ConfigAdapter.DOUBLE.convert(value.second!!)
                RecipeInput.of(fluid, amount)
            }

            else -> throw IllegalArgumentException("Cannot convert $value to fluid recipe input")
        }
    }
}