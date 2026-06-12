package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.recipe.ingredient.FluidWithAmount
import org.bukkit.configuration.ConfigurationSection

object FluidWithAmountConfigAdapter : ConfigAdapter<FluidWithAmount> {

    override val type = FluidWithAmount::class.java

    override fun convert(value: Any): FluidWithAmount = when (value) {
        is Pair<*, *> -> FluidWithAmount(
                ConfigAdapter.REBAR_FLUID.convert(value.first!!),
                ConfigAdapter.DOUBLE.convert(value.second!!)
        )
        is ConfigurationSection, is Map<*, *> -> {
            val map = MapConfigAdapter.STRING_TO_ANY.convert(value)
            check(map.size == 1) { "The input section had more than one key. Ensure the config is in the format e.g. `pylon:water: 500`" }
            convert(map.entries.first().toPair())
        }
        else -> throw IllegalArgumentException("Cannot convert $value to FluidOrItem.Fluid")
    }
}
