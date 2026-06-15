package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.item.ItemTypeWrapper
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.RecipeChoice

object RecipeChoiceConfigAdapter: ConfigAdapter<RecipeChoice> {
    override val type = RecipeChoice::class.java

    override fun convert(value: Any): RecipeChoice {
        return when (value) {
            is ConfigurationSection, is Map<*, *> -> convert(
                MapConfigAdapter.STRING_TO_ANY.convert(value).toList().single()
            )

            is Pair<*, *> -> {
                val ingredient = ConfigAdapter.STRING.convert(value.first!!)
                val amount = ConfigAdapter.INTEGER.convert(value.second!!)
                if (ingredient.startsWith("#")) {
                    val tag = ConfigAdapter.ITEM_TAG.convert(value.first!!)
                    val exact = amount > 1 || tag.values.any { it is ItemTypeWrapper.Rebar }
                    if (exact) {
                        RecipeChoice.ExactChoice(tag.values.map { it.createItemStack(amount) })
                    } else {
                        RecipeChoice.MaterialChoice(tag.values.map { (it as ItemTypeWrapper.Vanilla).material })
                    }
                } else if (ingredient.contains("[")) {
                    val stack = ConfigAdapter.ITEM_STACK.convert(value)
                    stack.amount = amount
                    RecipeChoice.ExactChoice(stack)
                } else {
                    val type = ConfigAdapter.ITEM_TYPE_WRAPPER.convert(ingredient)
                    if (amount > 1 || type is ItemTypeWrapper.Rebar) {
                        RecipeChoice.ExactChoice(type.createItemStack(amount))
                    } else {
                        RecipeChoice.MaterialChoice((type as ItemTypeWrapper.Vanilla).material)
                    }
                }
            }

            is String -> {
                if (value.startsWith("#")) {
                    convert(value to 1)
                } else if (value.contains("[")) {
                    val stack = ConfigAdapter.ITEM_STACK.convert(value)
                    RecipeChoice.ExactChoice(stack)
                } else {
                    val type = ConfigAdapter.ITEM_TYPE_WRAPPER.convert(value)
                    if (type is ItemTypeWrapper.Rebar) {
                        RecipeChoice.ExactChoice(type.createItemStack())
                    } else {
                        RecipeChoice.MaterialChoice((type as ItemTypeWrapper.Vanilla).material)
                    }
                }
            }
            else -> throw IllegalArgumentException("Cannot convert $value to recipe choice")
        }
    }
}