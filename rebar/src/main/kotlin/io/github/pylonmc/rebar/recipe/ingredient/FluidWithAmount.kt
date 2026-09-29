package io.github.pylonmc.rebar.recipe.ingredient

import io.github.pylonmc.rebar.fluid.RebarFluid
import io.github.pylonmc.rebar.guide.button.FluidButton

@JvmRecord
data class FluidWithAmount(val fluid: RebarFluid, val amount: Double) : FluidOrItem {

    fun addAmount(amount: Double) = copy(amount = this.amount + amount)
    fun subtractAmount(amount: Double) = copy(amount = this.amount - amount)

    override fun getKey() = fluid.key
    override fun matchesType(other: FluidOrItem) = other is FluidWithAmount && this.fluid == other.fluid
    override fun button() = FluidButton.of(this)
}