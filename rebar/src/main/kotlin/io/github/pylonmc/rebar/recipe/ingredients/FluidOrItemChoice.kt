package io.github.pylonmc.rebar.recipe.ingredients

import xyz.xenondevs.invui.item.Item

/**
 * A marker interface which is only implemented by [FluidChoice] or [ItemChoice].
 *
 * Exists only to make it easier to pass around objects which could either be a [FluidChoice] or [ItemChoice].
 */
sealed interface FluidOrItemChoice {
    fun button(): Item
}