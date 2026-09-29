package io.github.pylonmc.rebar.recipe.ingredient

import io.github.pylonmc.rebar.fluid.RebarFluid
import io.github.pylonmc.rebar.guide.button.FluidButton
import org.jetbrains.annotations.Contract
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Represents a fluid input to a recipe.
 */
@OptIn(ExperimentalContracts::class)
class FluidChoice private constructor(val fluids: Set<RebarFluid>, val amount: Double) : FluidOrItemChoice {

    @Contract("null, _ -> false")
    fun matches(fluid: RebarFluid?, amount: Double): Boolean {
        contract { returns(true) implies (fluid != null) }
        return fluid != null && amount >= this.amount && fluid in fluids
    }

    @Contract("null -> false")
    fun matchesIgnoringAmount(fluid: RebarFluid?): Boolean {
        contract { returns(true) implies (fluid != null) }
        return fluid != null && fluid in fluids
    }

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

        /**
         * Creates a [FluidChoice] which accepts at least the given amount of the given fluid
         */
        @JvmStatic
        fun of(fluidWithAmount: FluidWithAmount) = of(fluidWithAmount.fluid, fluidWithAmount.amount)
    }
}