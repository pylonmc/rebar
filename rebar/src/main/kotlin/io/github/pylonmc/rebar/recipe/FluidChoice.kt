package io.github.pylonmc.rebar.recipe

import io.github.pylonmc.rebar.fluid.RebarFluid
import io.github.pylonmc.rebar.guide.button.FluidButton

/**
 * Represents a fluid input to a recipe.
 */
class FluidChoice private constructor(val fluids: Map<RebarFluid, Double>) : FluidOrItemChoice {

    fun matches(fluid: RebarFluid, amount: Double) = fluids[fluid]?.let { amount >= it } ?: false

    override fun button() = FluidButton.of(this)

    companion object {

        /**
         * Creates a [FluidChoice] which accepts the corresponding amount of fluid for each fluid
         * in the provided [fluids] map.
         */
        fun of(fluids: Map<RebarFluid, Double>) = FluidChoice(fluids)

        /**
         * Creates a [FluidChoice] which accepts [amount] (or greater) of the given [fluid].
         */
        fun of(fluid: RebarFluid, amount: Double) = of(mapOf(fluid to amount))

        /**
         * Creates a [FluidChoice] which accepts [amount] (or greater) of any of the given [fluids].
         */
        fun of(fluids: List<RebarFluid>, amount: Double) = of(fluids.associateWith { amount })
    }
}