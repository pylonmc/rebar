package io.github.pylonmc.rebar.recipe

import io.github.pylonmc.rebar.guide.button.ItemButton
import io.github.pylonmc.rebar.item.ItemTypeWrapper
import io.github.pylonmc.rebar.logistics.slot.LogisticSlot
import io.github.pylonmc.rebar.util.componentsEqual
import io.github.pylonmc.rebar.util.hasDefaultComponents
import io.github.pylonmc.rebar.util.isDefaultComponents
import io.github.pylonmc.rebar.util.matchesComponents
import io.github.pylonmc.rebar.util.overriddenComponents
import io.github.pylonmc.rebar.util.overriddenDataTypes
import io.papermc.paper.datacomponent.DataComponentType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import java.util.function.Predicate

/**
 * Represents an item being used in a recipe.
 *
 * @see ItemChoice.Builder
 */
class ItemChoice internal constructor(
    @JvmSynthetic
    internal val internalChoices: List<InternalItemChoice>,
    val amount: Int,
) : FluidOrItemChoice {

    val representativeItems: List<ItemStack> = internalChoices.map { it.wrapper.createItemStack(amount) }

    internal class InternalItemChoice(
        val wrapper: ItemTypeWrapper,
        val predicate: Predicate<ItemStack>? = null,
    ) {
        fun matches(stack: ItemStack): Boolean
                = wrapper.matches(stack) && predicate?.test(stack) ?: true

        fun matches(slot: LogisticSlot) = slot.getItemStack()?.let { matches(it) } ?: false
    }

    fun matchesIgnoringAmount(stack: ItemStack)
            = internalChoices.any { it.matches(stack) }

    fun matchesIgnoringAmount(slot: LogisticSlot) = slot.getItemStack()?.let { stack ->
        internalChoices.any { choice -> choice.matches(stack) }
    } ?: false

    fun matches(stack: ItemStack?)
            = stack != null && stack.amount >= amount && matchesIgnoringAmount(stack)

    fun matches(slot: LogisticSlot)
            = slot.getAmount() >= amount && matchesIgnoringAmount(slot)

    override fun button() = ItemButton.of(this)

    /**
     * Returns a recipe choice which REPRESENTS this ItemChoice
     *
     * This is not directly used to match recipes because it cannot represent all the possible
     * inputs that [ItemChoice] can.
     *
     * @see RecipeCompletion
     */
    @JvmSynthetic
    internal fun toRepresentativeRecipeChoice(): RecipeChoice {
        val allUnmodifiedMaterials = internalChoices.all { it.wrapper is ItemTypeWrapper.Vanilla && it.predicate == null }
        if (allUnmodifiedMaterials) {
            return RecipeChoice.MaterialChoice(internalChoices.map { (it.wrapper as ItemTypeWrapper.Vanilla).material })
        }
        return RecipeChoice.ExactChoice(representativeItems)
    }

    /**
     * TODO
     */
    @JvmSynthetic
    internal fun toDummyRecipeChoice(): RecipeChoice {
        return RecipeChoice.MaterialChoice(internalChoices.map {
            when(val type = it.wrapper) {
                is ItemTypeWrapper.Vanilla -> type.material
                is ItemTypeWrapper.Rebar -> type.item.getOriginalTemplate().type
            }
        })
    }

    /**
     * Builds an [ItemChoice] which requires [amount] items. Accepts nothing by default.
     */
    class Builder {

        internal val internalChoices = mutableListOf<InternalItemChoice>()
        private var amount: Int = 1

        fun amount(amount: Int) = apply { this.amount = amount }

        private fun add(internalChoice: InternalItemChoice) = apply { internalChoices.add(internalChoice) }

        /**
         * Matches stacks of [type] which match the given [predicate]
         *
         * Checks that the components of any item being compared are the default ones for this [type].
         */
        private fun add(type: ItemTypeWrapper, predicate: Predicate<ItemStack>? = null) = add(
            InternalItemChoice(type, predicate)
        )

        /**
         * Matches stacks of the given [type].
         *
         * Does not check that the components of any item being compared are the default ones for this [type].
         */
        fun addFuzzy(type: ItemTypeWrapper) = add(type)

        /**
         * Matches stacks of the given [type].
         *
         * Only checks that the provided [components] of any item being compared match the [type]'s default
         * values.
         */
        fun addFuzzy(type: ItemTypeWrapper, components: Set<DataComponentType>) = if (components.isEmpty()) {
            addFuzzy(type)
        } else {
            add(type) { stack ->
                stack.hasDefaultComponents(components)
            }
        }

        /**
         * Matches stacks containing at least [amount] of the given [type].
         *
         * Checks that the provided [components] of any item being compared match the corresponding value.
         */
        fun addFuzzy(type: ItemTypeWrapper, components: Map<DataComponentType, Any?>) = if (components.isEmpty()) {
            addFuzzy(type)
        } else {
            add(type) { stack ->
                stack.matchesComponents(components)
            }
        }

        /**
         * Matches stacks containing at least [amount] of the given [stack]'s type
         *
         * Only checks the provided [stack]'s overriden components of any item being compared
         */
        fun addFuzzy(stack: ItemStack) = apply {
            val components = stack.overriddenComponents(false)
            addFuzzy(ItemTypeWrapper(stack), components)
        }

        /**
         * Matches stacks containing at least [amount] of the given [type] or 1 if [amount] is not provided.
         *
         * Checks that the components of any item being compared are the default ones for this [type].
         */
        fun addExact(type: ItemTypeWrapper) = add(type) {
            stack -> stack.isDefaultComponents
        }

        /**
         * Matches stacks containing at least [amount] of the given [type].
         *
         * Checks that the components of any item being compared are the default ones for this [type], except for
         * the provided [componentsToIgnore].
         */
        fun addExact(type: ItemTypeWrapper, componentsToIgnore: Set<DataComponentType>) = if (componentsToIgnore.isEmpty()) {
            addExact(type)
        } else {
            add(type) {
                    stack -> stack.overriddenDataTypes().subtract(componentsToIgnore).isEmpty()
            }
        }

        /**
         * Matches stacks containing at least [amount] of the given [stack].
         *
         * Checks that all components of any item being compared match the components of [stack], except for
         * the provided [componentsToIgnore].
         */
        @JvmOverloads
        fun addExact(stack: ItemStack, componentsToIgnore: Set<DataComponentType> = emptySet()) = apply {
            val components = stack.overriddenComponents(true).filterKeys { it !in componentsToIgnore }
            if (components.isEmpty()) {
                addFuzzy(ItemTypeWrapper(stack))
            } else {
                add(ItemTypeWrapper(stack)) { stack ->
                    stack.componentsEqual(components)
                }
            }
        }

        fun build() = ItemChoice(internalChoices, amount)
    }

    companion object {
        fun matches(stack: ItemStack?, choice: ItemChoice?): Boolean {
            if (choice == null) {
                return stack == null || stack.isEmpty
            } else if (stack == null) {
                return false
            }
            return choice.matches(stack)
        }
    }
}