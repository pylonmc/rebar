package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.recipe.FluidOrItem

object FluidWithAmountConfigAdapter : ConfigAdapter<FluidOrItem.Fluid> {

    override val type = FluidOrItem.Fluid::class.java

    override fun convert(value: Any): FluidOrItem.Fluid = when (value) {
        is Pair<*, *> -> FluidOrItem.Fluid(
                ConfigAdapter.REBAR_FLUID.convert(value.first!!),
                ConfigAdapter.DOUBLE.convert(value.second!!)
        )
        else -> throw IllegalArgumentException("Cannot convert $value to FluidOrItem.Fluid")
    }
}
