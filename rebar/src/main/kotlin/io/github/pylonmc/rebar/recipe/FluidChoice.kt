package io.github.pylonmc.rebar.recipe

import io.github.pylonmc.rebar.fluid.RebarFluid
import io.github.pylonmc.rebar.guide.button.FluidButton

/**
 * Represents a fluid input to a recipe.
 */
class FluidChoice private constructor(val fluids: Set<RebarFluid>, val amount: Double) : FluidOrItemChoice {

    fun matches(fluid: RebarFluid, amount: Double) = amount >= this.amount && fluids.contains(fluid)

    fun matchesIgnoringAmount(fluid: RebarFluid) = fluids.contains(fluid)

    override fun button() = FluidButton.of(this)

    companion object {

        /**
         * Creates a [FluidChoice] which accepts the corresponding amount of fluid for each fluid
         * in the provided [fluids] map.
         */
        @JvmStatic
        fun of(fluids: Set<RebarFluid>, amount: Double) = FluidChoice(fluids, amount)

        /**
         * Creates a [FluidChoice] which accepts [amount] (or greater) of the given [fluid].
         */
        @JvmStatic
        fun of(fluid: RebarFluid, amount: Double) = of(setOf(fluid), amount)
    }
}