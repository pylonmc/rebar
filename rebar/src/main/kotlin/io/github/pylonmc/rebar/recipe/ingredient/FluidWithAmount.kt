package io.github.pylonmc.rebar.recipe.ingredient

import io.github.pylonmc.rebar.fluid.RebarFluid
import io.github.pylonmc.rebar.guide.button.FluidButton

@JvmRecord
data class FluidWithAmount(val fluid: RebarFluid, val amountMillibuckets: Double) : FluidOrItem {

    override fun getKey() = fluid.key
    override fun matchesType(other: FluidOrItem) = other is FluidWithAmount && this.fluid == other.fluid
    override fun button() = FluidButton.of(this)
}