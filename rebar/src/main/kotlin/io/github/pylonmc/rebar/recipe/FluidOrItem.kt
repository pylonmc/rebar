package io.github.pylonmc.rebar.recipe

import io.github.pylonmc.rebar.fluid.RebarFluid
import io.github.pylonmc.rebar.guide.button.ItemButton
import io.github.pylonmc.rebar.item.RebarItemSchema
import io.github.pylonmc.rebar.util.rebarTypeSimilar
import org.bukkit.Keyed
import org.bukkit.inventory.ItemStack

import xyz.xenondevs.invui.item.Item as UIItem

/**
 * A wrapper which is either a [FluidOrItem.Item] wrapping an [ItemStack], or a [FluidWithAmount].
 */
sealed interface FluidOrItem : Keyed {

    fun matchesType(other: FluidOrItem): Boolean
    fun button(): UIItem

    /**
     * A simple wrapper around [ItemStack]
     */
    @JvmRecord
    data class Item(val item: ItemStack) : FluidOrItem {
        override fun getKey() = RebarItemSchema.fromStack(item)?.key ?: item.type.key
        override fun matchesType(other: FluidOrItem) = other is Item && rebarTypeSimilar(this.item, other.item)
        override fun button() = ItemButton.of(this)

        companion object {
            val EMPTY = Item(ItemStack.empty())
        }
    }


    companion object {

        @JvmStatic
        fun of(item: ItemStack): Item = Item(item)

        @JvmStatic
        fun of(fluid: RebarFluid, amountMillibuckets: Double)
                = FluidWithAmount(fluid, amountMillibuckets)

    }
}