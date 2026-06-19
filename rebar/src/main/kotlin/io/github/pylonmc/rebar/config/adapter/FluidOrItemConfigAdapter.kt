package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.recipe.ingredient.FluidOrItem

object FluidOrItemConfigAdapter : ConfigAdapter<FluidOrItem> {

    override val type = FluidOrItem::class.java

    override fun convert(value: Any): FluidOrItem {
        val errors = mutableListOf<Throwable>()
        runCatching { ConfigAdapter.ITEM_STACK.convert(value) }.onSuccess { return FluidOrItem.of(it) }.onFailure { errors.add(it) }
        runCatching { FluidWithAmountConfigAdapter.convert(value) }.onSuccess { return it }.onFailure { errors.add(it) }
        check(errors.size == 2)
        Rebar.logger.severe("Cannot convert $value to FluidOrItem.")
        Rebar.logger.severe("Error thrown when attempting to convert to item:")
        errors[0].printStackTrace()
        Rebar.logger.severe("Error thrown when attempting to convert to fluid:")
        errors[1].printStackTrace()
        throw IllegalArgumentException("Failed to convert $value to FluidOrItem; more details above this message",)
    }
}