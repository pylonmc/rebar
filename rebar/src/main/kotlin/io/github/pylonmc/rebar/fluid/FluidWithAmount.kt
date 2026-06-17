package io.github.pylonmc.rebar.fluid

import io.github.pylonmc.rebar.recipe.FluidOrItem

@JvmRecord
data class FluidWithAmount(val fluid: RebarFluid, val millibuckets: Double) {
    fun asFluidOrItem(): FluidOrItem = FluidOrItem.Fluid(fluid, millibuckets)

    fun withFluid(fluid: RebarFluid): FluidWithAmount = copy(fluid = fluid)
    fun withMillibuckets(amount: Double): FluidWithAmount = copy(millibuckets = amount)

    fun addMillibuckets(amount: Double): FluidWithAmount = copy(millibuckets = this.millibuckets + amount)
    fun subtractMillibuckets(amount: Double): FluidWithAmount = copy(millibuckets = this.millibuckets - amount)
}
