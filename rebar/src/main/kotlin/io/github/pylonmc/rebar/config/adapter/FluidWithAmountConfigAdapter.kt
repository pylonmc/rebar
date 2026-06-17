package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.fluid.FluidWithAmount
import org.bukkit.configuration.ConfigurationSection

object FluidWithAmountConfigAdapter : ConfigAdapter<FluidWithAmount> {

    override val type = FluidWithAmount::class.java

    override fun convert(value: Any): FluidWithAmount {
        return when (value) {
            is Pair<*, *> -> {
                val fluid = ConfigAdapter.REBAR_FLUID.convert(value.first!!)
                val amount = ConfigAdapter.DOUBLE.convert(value.second!!)
                FluidWithAmount(fluid, amount)
            }

            is ConfigurationSection, is Map<*, *> -> convert(MapConfigAdapter.STRING_TO_ANY.convert(value).toList().single())
            else -> throw IllegalArgumentException("Cannot convert $value to FluidWithAmount")
        }
    }
}