package io.github.pylonmc.rebar.fluid

import net.kyori.adventure.text.Component

/**
 * Fluid properties describe a fluid, such as its temperature or whether it can be used as a coolant.
 */
interface RebarFluidProperty {
    /**
     * A component which is added to the lore of fluids to give information about this property.
     *
     * For example, for temperatures, this should show 'Temperature: Cold' or similar.
     */
    val displayText: Component
}