package io.github.pylonmc.rebar.fluid

import io.github.pylonmc.rebar.recipe.FluidOrItem

@JvmRecord
data class FluidWithAmount(val fluid: RebarFluid, val amount: Double) {
    fun asFluidOrItem(): FluidOrItem = FluidOrItem.Fluid(fluid, amount)

    fun withFluid(fluid: RebarFluid): FluidWithAmount = copy(fluid = fluid)
    fun withAmount(amount: Double): FluidWithAmount = copy(amount = amount)

    fun addAmount(amount: Double): FluidWithAmount = copy(amount = this.amount + amount)
    fun subtractAmount(amount: Double): FluidWithAmount = copy(amount = this.amount - amount)
}
