package io.github.pylonmc.rebar.recipe

import com.google.common.collect.MapMaker
import io.github.pylonmc.rebar.content.guide.RebarGuide
import io.github.pylonmc.rebar.fluid.RebarFluid
import io.github.pylonmc.rebar.recipe.ingredient.FluidChoice
import io.github.pylonmc.rebar.recipe.ingredient.FluidOrItem
import io.github.pylonmc.rebar.recipe.ingredient.FluidOrItemChoice
import io.github.pylonmc.rebar.recipe.ingredient.FluidWithAmount
import io.github.pylonmc.rebar.recipe.ingredient.ItemChoice
import org.bukkit.Keyed
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.gui.Gui

/**
 * A generic rebar recipe
 *
 * @property isHidden If this recipe should be hidden from the [RebarGuide]
 * @property inputs The inputs used by this recipe, either [FluidChoice] or [ItemChoice]
 * @property results The results of this recipe, either [FluidWithAmount] or [FluidOrItem.Item]
 *
 * @see RecipeType
 */
interface RebarRecipe : Keyed {

    val isHidden: Boolean
        get() = false

    val inputs: List<FluidOrItemChoice>
    val results: List<FluidOrItem>

    /**
     * Returns whether [stack] is used anywhere by this recipe.
     *
     * Primarily used for guide navigation
     */
    fun isInput(stack: ItemStack) = inputs.any { input ->
        when (input) {
            is ItemChoice -> input.matches(stack)
            else -> false
        }
    }

    /**
     * Returns whether [fluid] is used anywhere by this recipe.
     *
     * Primarily used for guide navigation
     */
    fun isInput(fluid: RebarFluid) = inputs.any { input ->
        when (input) {
            is FluidChoice -> fluid in input.fluids
            else -> false
        }
    }

    /**
     * Returns whether [stack] is a result of this recipe.
     *
     * Primarily used for guide navigation
     */
    fun isOutput(stack: ItemStack) = results.any {
        when (it) {
            is FluidOrItem.Item -> it.item.isSimilar(stack)
            else -> false
        }
    }

    /**
     * Returns whether [fluid] is a result of this recipe.
     *
     * Primarily used for guide navigation
     */
    fun isOutput(fluid: RebarFluid) = results.any {
        when (it) {
            is FluidWithAmount -> it.fluid == fluid
            else -> false
        }
    }

    fun display(): Gui?

    companion object {
        private val priorities = MapMaker().weakKeys().makeMap<RebarRecipe, Double>()

        @JvmStatic
        var RebarRecipe.priority: Double
            get() = priorities.getOrDefault(this, 0.0)
            set(value) {
                priorities[this] = value
            }
    }
}
