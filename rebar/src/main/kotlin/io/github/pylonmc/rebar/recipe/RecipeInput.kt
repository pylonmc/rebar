package io.github.pylonmc.rebar.recipe

import io.github.pylonmc.rebar.fluid.RebarFluid
import io.github.pylonmc.rebar.item.ItemTypeWrapper
import io.github.pylonmc.rebar.util.forceSetData
import io.github.pylonmc.rebar.util.overriddenDataTypes
import io.github.pylonmc.rebar.util.setComponents
import io.papermc.paper.datacomponent.DataComponentType
import net.kyori.adventure.text.minimessage.translation.Argument.tag
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.inventory.ItemStack
import kotlin.collections.associate
import kotlin.collections.emptyMap
import kotlin.to

sealed interface RecipeInput {
    @Suppress("UnstableApiUsage")
    data class Item(val items: List<ItemTypeWrapper>, val components: List<Map<DataComponentType, Any?>>, val amount: Int) : RecipeInput {
        constructor(items: List<ItemTypeWrapper>, amount: Int) : this(items.toList(), mutableListOf(), amount)
        constructor(amount: Int, vararg items: ItemStack) : this(
            items.mapTo(mutableListOf()) { ItemTypeWrapper(it) },
            items.mapTo(mutableListOf()) { it.overriddenDataTypes().associateWith { type ->
                return@associateWith when(type) {
                    is DataComponentType.Valued<*> -> it.getData(type)
                    else -> null
                }
            } },
            amount
        )
        constructor(tag: Tag<ItemTypeWrapper>, amount: Int) : this(tag.values.toList(), amount)

        init {
            require(amount > 0) { "Amount must be greater than zero, but was $amount" }
            require(items.isNotEmpty()) { "Items set must not be empty" }
            require(components.isEmpty() || items.size == components.size) { "Components must be empty of be the same size as items" }
        }

        val representativeItems: Set<ItemStack> by lazy {
            items.mapIndexedTo(mutableSetOf()) { i, type ->
                val components = components.getOrNull(i).orEmpty()
                type.createItemStack().apply {
                    this.amount = this@Item.amount
                    setComponents(components)
                }
            }
        }

        val representativeItem: ItemStack by lazy {
            representativeItems.first()
        }

        fun matches(itemStack: ItemStack?): Boolean {
            if (itemStack == null || itemStack.amount < amount) return false
            return matchesIgnoringAmount(itemStack)
        }

        fun matchesIgnoringAmount(itemStack: ItemStack?): Boolean {
            itemStack ?: return false
            val index = items.indexOf(ItemTypeWrapper(itemStack))
            if (index == -1) return false

            val components = components.getOrNull(index)
            if (components.isNullOrEmpty()) return true

            for (entry in components) {
                when (val key = entry.key) {
                    is DataComponentType.NonValued -> if (!itemStack.hasData(key)) return false
                    is DataComponentType.Valued<*> -> {
                        if (entry.value != itemStack.getData(key)) {
                            return false
                        }
                    }
                }
            }

            return true
        }
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