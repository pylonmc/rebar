package io.github.pylonmc.rebar.recipe

import io.github.pylonmc.rebar.guide.button.ItemButton
import io.github.pylonmc.rebar.item.ItemTypeWrapper
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
class ItemChoice private constructor() : FluidOrItemChoice {

    @JvmSynthetic
    internal val internalChoices: MutableList<InternalItemChoice> = mutableListOf()

    internal class InternalItemChoice(
        val wrapper: ItemTypeWrapper,
        val amount: Int,
        val predicate: Predicate<ItemStack>? = null,
        val representativeItem: ItemStack = wrapper.createItemStack(amount)
    ) {
        fun validate(stack: ItemStack): Boolean
                = stack.amount >= amount && wrapper.matches(stack) && predicate?.test(stack) ?: true
    }

    fun validate(stack: ItemStack)
            = internalChoices.any { it.validate(stack) }
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
        return RecipeChoice.ExactChoice(internalChoices.map { it.representativeItem })
    }

    val representativeItems: List<ItemStack> = internalChoices.map { it.representativeItem }

    /**
     * Builds an [ItemChoice]. Accepts nothing by default.
     */
    class Builder(private val choice: ItemChoice = ItemChoice()) {

        private fun add(internalChoice: InternalItemChoice) = apply { choice.internalChoices.add(internalChoice) }

        /**
         * Matches stacks of [type] which match the given [predicate]
         *
         * Checks that the components of any item being compared are the default ones for this [type].
         */
        private fun add(type: ItemTypeWrapper, predicate: Predicate<ItemStack>? = null) = add(
            InternalItemChoice(type, 1, predicate)
        )

        /**
         * Matches stacks of [type] containing at least [amount] which match the given [predicate]
         *
         * Checks that the components of any item being compared are the default ones for this [type].
         */
        private fun add(type: ItemTypeWrapper, amount: Int, predicate: Predicate<ItemStack>? = null) = add(
            InternalItemChoice(type, amount, predicate)
        )

        /**
         * Matches stacks containing at least [amount] of the given [type] or 1 if not provided.
         *
         * Does not check that the components of any item being compared are the default ones for this [type].
         */
        @JvmOverloads
        fun addFuzzy(type: ItemTypeWrapper, amount: Int = 1) = add(type, amount)

        /**
         * Matches stacks containing at least [amount] of the given [type].
         *
         * Only checks that the provided [components] of any item being compared match the [type]'s default
         * values.
         */
        fun addFuzzy(type: ItemTypeWrapper, amount: Int, components: Set<DataComponentType>) = if (components.isEmpty()) {
            addFuzzy(type, amount)
        } else {
            add(type, amount) { stack ->
                stack.hasDefaultComponents(components)
            }
        }

        /**
         * Matches stacks containing at least [amount] of the given [type].
         *
         * Checks that the provided [components] of any item being compared match the corresponding value.
         */
        fun addFuzzy(type: ItemTypeWrapper, amount: Int, components: Map<DataComponentType, Any?>) = if (components.isEmpty()) {
            addFuzzy(type, amount)
        } else {
            add(type, amount) { stack ->
                stack.matchesComponents(components)
            }
        }

        /**
         * Matches stacks containing at least [amount] of the given [stack]'s type
         *
         * Only checks the provided [stack]'s overriden components of any item being compared
         */
        fun addFuzzy(stack: ItemStack, amount: Int) = apply {
            val components = stack.overriddenComponents(false)
            addFuzzy(ItemTypeWrapper(stack), amount, components)
        }

        /**
         * Matches stacks containing at least [amount] of the given [type] or 1 if [amount] is not provided.
         *
         * Checks that the components of any item being compared are the default ones for this [type].
         */
        @JvmOverloads
        fun addExact(type: ItemTypeWrapper, amount: Int = 1) = add(type, amount) {
            stack -> stack.isDefaultComponents
        }

        /**
         * Matches stacks containing at least [amount] of the given [type].
         *
         * Checks that the components of any item being compared are the default ones for this [type], except for
         * the provided [componentsToIgnore].
         */
        fun addExact(type: ItemTypeWrapper, amount: Int, componentsToIgnore: Set<DataComponentType>) = if (componentsToIgnore.isEmpty()) {
            addExact(type, amount)
        } else {
            add(type, amount) {
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
        fun addExact(stack: ItemStack, amount: Int, componentsToIgnore: Set<DataComponentType> = emptySet()) = apply {
            val components = stack.overriddenComponents(true).filterKeys { it !in componentsToIgnore }
            if (components.isEmpty()) {
                addFuzzy(ItemTypeWrapper(stack), amount)
            } else {
                add(ItemTypeWrapper(stack)) { stack ->
                    stack.amount >= amount && stack.componentsEqual(components)
                }
            }
        }

        fun build() = choice
    }

    companion object {
        fun validate(stack: ItemStack?, choice: ItemChoice?): Boolean {
            if (choice == null) {
                return stack == null || stack.isEmpty
            } else if (stack == null) {
                return false
            }
            return choice.validate(stack)
        }
    }
}