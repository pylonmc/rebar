package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.recipe.FluidOrItem

object FluidOrItemConfigAdapter : ConfigAdapter<FluidOrItem> {

    override val type = FluidOrItem::class.java

    override fun convert(value: Any): FluidOrItem {
        runCatching { ConfigAdapter.ITEM_STACK.convert(value) }.onSuccess { return FluidOrItem.of(it) }
        runCatching { FluidWithAmountConfigAdapter.convert(value) }.onSuccess { return it }
        throw IllegalArgumentException("Cannot convert $value to FluidOrItem")
    }
}