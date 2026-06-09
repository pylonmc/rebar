package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.recipe.FluidWithAmount

object FluidWithAmountConfigAdapter : ConfigAdapter<FluidWithAmount> {

    override val type = FluidWithAmount::class.java

    override fun convert(value: Any): FluidWithAmount = when (value) {
        is Pair<*, *> -> FluidWithAmount(
                ConfigAdapter.REBAR_FLUID.convert(value.first!!),
                ConfigAdapter.DOUBLE.convert(value.second!!)
        )
        else -> throw IllegalArgumentException("Cannot convert $value to FluidOrItem.Fluid")
    }
}
