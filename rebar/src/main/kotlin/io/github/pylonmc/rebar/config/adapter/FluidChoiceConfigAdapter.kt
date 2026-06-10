package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.recipe.FluidChoice
import org.bukkit.configuration.ConfigurationSection

object FluidChoiceConfigAdapter : ConfigAdapter<FluidChoice> {

    override val type = FluidChoice::class.java

    override fun convert(value: Any): FluidChoice = when (value) {
        is Pair<*, *> -> FluidChoice.of(
            // e.g. 'pylon:water: 100'
            ConfigAdapter.REBAR_FLUID.convert(value.first!!),
            ConfigAdapter.DOUBLE.convert(value.second!!)
        )
        is ConfigurationSection, is Map<*, *> -> {
            val map = MapConfigAdapter.STRING_TO_ANY.convert(value)
            if (map.size == 1) {
                // e.g.
                // input:
                //  pylon:water: 500
                return convert(map.entries.first().toPair())
            }

            // e.g.
            // input:
            //   fluids: [pylon:water, pylon:lava]
            //   amount: 300
            val fluids = ConfigAdapter.SET.from(ConfigAdapter.REBAR_FLUID)
                .convert(map["fluids"] ?: error("A list of fluids must be provided e.g. 'fluids: [pylon:water]'"))
            val amount = ConfigAdapter.DOUBLE.convert(map["amount"] ?: error("A fluid amount must be specified e.g. 'amount: 500'"))
            FluidChoice.of(fluids, amount)
        }
        else -> throw IllegalArgumentException("Cannot convert $value to FluidChoice")
    }
}
