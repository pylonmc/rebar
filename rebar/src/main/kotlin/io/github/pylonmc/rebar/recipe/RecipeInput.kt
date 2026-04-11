package io.github.pylonmc.rebar.recipe

import io.github.pylonmc.rebar.fluid.RebarFluid
import io.github.pylonmc.rebar.item.ItemTypeWrapper
import org.bukkit.Tag
import org.bukkit.inventory.ItemStack

sealed interface RecipeInput {
    data class Item(val items: MutableSet<ItemTypeWrapper>, val amount: Int) : RecipeInput {
        constructor(amount: Int, vararg items: ItemStack) : this(items.mapTo(mutableSetOf()) { ItemTypeWrapper(it) }, amount)
        constructor(tag: Tag<ItemTypeWrapper>, amount: Int) : this(tag.values, amount)

        init {
            require(amount > 0) { "Amount must be greater than zero, but was $amount" }
            require(items.isNotEmpty()) { "Items set must not be empty" }
        }

        val representativeItems: Set<ItemStack> by lazy {
            items.mapTo(mutableSetOf()) { it.createItemStack().asQuantity(amount) }
        }

        val representativeItem: ItemStack by lazy {
            representativeItems.first()
        }

        fun matches(itemStack: ItemStack?): Boolean {
            if (itemStack == null || itemStack.amount < amount) return false
            return contains(itemStack)
        }

        operator fun contains(itemStack: ItemStack?): Boolean = itemStack != null && ItemTypeWrapper(itemStack) in items
    }

    @JvmRecord
    data class Fluid(val fluids: MutableSet<RebarFluid>, val amountMillibuckets: Double) : RecipeInput {
        constructor(amountMillibuckets: Double, vararg fluids: RebarFluid) : this(fluids.toMutableSet(), amountMillibuckets)
        constructor(amountMillibuckets: Double, tag: Tag<RebarFluid>) : this(tag.values, amountMillibuckets)

        init {
            require(amountMillibuckets > 0) { "Amount in millibuckets must be greater than zero, but was $amountMillibuckets" }
            require(fluids.isNotEmpty()) { "Fluids set must not be empty" }
        }

        fun matches(fluid: RebarFluid?, amountMillibuckets: Double): Boolean {
            if (fluid == null || amountMillibuckets < this.amountMillibuckets) return false
            return contains(fluid)
        }

        operator fun contains(fluid: RebarFluid?): Boolean = fluid in fluids
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun of(item: ItemStack, amount: Int = item.amount) = Item(amount, item)

        @JvmStatic
        fun of(fluid: RebarFluid, amountMillibuckets: Double) = Fluid(amountMillibuckets, fluid)
    }
}