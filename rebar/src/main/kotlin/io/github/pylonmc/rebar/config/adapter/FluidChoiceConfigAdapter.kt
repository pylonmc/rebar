package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.recipe.FluidChoice
import org.bukkit.configuration.ConfigurationSection

object FluidChoiceConfigAdapter : ConfigAdapter<FluidChoice> {

    override val type = FluidChoice::class.java

    override fun convert(value: Any): FluidChoice = when (value) {
        is ConfigurationSection, is Map<*, *> -> FluidChoice.of(
            MapConfigAdapter.STRING_TO_ANY.convert(value).toList().associate {
                val fluid = ConfigAdapter.REBAR_FLUID.convert(it.first)
                val amount = ConfigAdapter.DOUBLE.convert(it.second)
                fluid to amount
            }
        )
        else -> throw IllegalArgumentException("Cannot convert $value to FluidChoice")
    }
}
