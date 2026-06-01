package io.github.pylonmc.rebar.item.interfaces

/**
 * Allows the item to act as a normal vanilla fuel.
 *
 * For example, by default, a 'magic coal' item which has a material of coal cannot
 * be burnt in furnaces. However, if your magic coal item implements this interface,
 * it will be treated the same as a normal piece of coal when burnt as fuel.
 */
interface VanillaFurnaceFuel