package io.github.pylonmc.rebar.recipe

import com.google.common.collect.MapMaker
import io.github.pylonmc.rebar.fluid.RebarFluid
import io.github.pylonmc.rebar.recipe.ingredients.FluidChoice
import io.github.pylonmc.rebar.recipe.ingredients.FluidOrItem
import io.github.pylonmc.rebar.recipe.ingredients.FluidOrItemChoice
import io.github.pylonmc.rebar.recipe.ingredients.FluidWithAmount
import io.github.pylonmc.rebar.recipe.ingredients.ItemChoice
import org.bukkit.Keyed
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.gui.Gui

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
