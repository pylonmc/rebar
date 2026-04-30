package io.github.pylonmc.rebar.fluid

import io.github.pylonmc.rebar.recipe.FluidOrItem

@JvmRecord
data class FluidWithAmount(val fluid: RebarFluid, val amount: Double) {
    fun asFluidOrItem(): FluidOrItem = FluidOrItem.Fluid(fluid, amount)
}
