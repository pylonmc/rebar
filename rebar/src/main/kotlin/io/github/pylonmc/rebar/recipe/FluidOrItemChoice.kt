package io.github.pylonmc.rebar.recipe

/**
 * A marker interface which is only implemented by [FluidChoice] or [ItemChoice].
 *
 * Exists only to make it easier to pass around objects which could either be a [FluidChoice] or [ItemChoice].
 */
sealed interface FluidOrItemChoice
