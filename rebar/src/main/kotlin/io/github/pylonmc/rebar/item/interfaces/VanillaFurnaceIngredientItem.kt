package io.github.pylonmc.rebar.item.interfaces

/**
 * Allows the item to act as a normal vanilla item when smelted.
 *
 * For example, by default, a 'magic cobblestone' item which has a material of cobblestone
 * cannot be smelted in a furnace. However, if your magic cobblestone item implements this
 * interface, it will be treated the same as a normal piece of cobblestone when you attempt
 * to smelt it.
 */
interface VanillaFurnaceIngredientItem