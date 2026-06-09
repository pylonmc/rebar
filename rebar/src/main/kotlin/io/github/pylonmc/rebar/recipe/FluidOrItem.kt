package io.github.pylonmc.rebar.recipe

import io.github.pylonmc.rebar.fluid.RebarFluid
import io.github.pylonmc.rebar.guide.button.FluidButton
import io.github.pylonmc.rebar.guide.button.ItemButton
import io.github.pylonmc.rebar.item.ItemTypeWrapper
import io.github.pylonmc.rebar.item.RebarItemSchema
import io.github.pylonmc.rebar.util.rebarTypeSimilar
import io.papermc.paper.datacomponent.item.attribute.AttributeModifierDisplay.override
import org.bukkit.Keyed
import org.bukkit.inventory.ItemStack

import xyz.xenondevs.invui.item.Item as UIItem

sealed interface FluidOrItem : Keyed {

    fun matchesType(other: FluidOrItem): Boolean
    fun button(): UIItem

    @JvmRecord
    data class Item(val item: ItemStack) : FluidOrItem {
        override fun getKey() = RebarItemSchema.fromStack(item)?.key ?: item.type.key
        override fun matchesType(other: FluidOrItem) = other is Item && rebarTypeSimilar(this.item, other.item)
        override fun button() = ItemButton.of(this)

        companion object {
            val EMPTY = Item(ItemStack.empty())
        }
    }

    @JvmRecord
    data class Fluid(val fluid: RebarFluid, val amountMillibuckets: Double) : FluidOrItem {
        override fun getKey() = fluid.key
        override fun matchesType(other: FluidOrItem) = other is Fluid && this.fluid == other.fluid
        override fun button() = FluidButton.of(this)
    }

    companion object {

        @JvmStatic
        fun of(item: ItemStack): Item = Item(item)

        @JvmStatic
        fun of(fluid: RebarFluid, amountMillibuckets: Double): Fluid = Fluid(fluid, amountMillibuckets)

    }
}