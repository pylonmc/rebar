package io.github.pylonmc.rebar.fluid.properties

import io.github.pylonmc.rebar.fluid.RebarFluidProperty
import io.github.pylonmc.rebar.i18n.RebarArgument
import net.kyori.adventure.text.Component

/**
 * Most pipes require fluids to have a temperature to be able to transfer fluids.
 */
enum class FluidTemperature : RebarFluidProperty {
    /**
     * Generally fluids that are considered cryogenic, like liquid nitrogen or liquid helium.
     */
    COLD,

    /**
     * Generally fluids that are liquid at room temperature, like water or oil.
     */
    NORMAL,

    /**
     * Generally fluids that would be considered hot, like molten metals or lava.
     */
    HOT;

    /**
     * The display name of this temperature (eg: 'Cold').
     */
    val valueText: Component = Component.translatable("rebar.fluid.temperature.${this.name.lowercase()}")

    override val displayText: Component = Component.translatable("rebar.fluid.temperature.display-text",
        RebarArgument.of("temperature", valueText)
    )
}
