package io.github.pylonmc.rebar.logistics.slot

import org.bukkit.inventory.ItemStack

/**
 * Represents a slot in an interface which can have items added or removed.
 */
interface LogisticSlot {

    /**
     * The amount of the returned [ItemStack] does not matter and will not
     * be used at any point. For specifying the item amount, use [getAmount].
     *
     * This allows for arbitrarily large amounts to be set, instead of being
     * constrained by the maximum amount of an ItemStack.
     *
     * WARNING: Callers to this function should not mutate the ItemStack
     * that is returned. This will likely lead to unexpected behaviour.
     */
    fun getItemStack(): ItemStack?

    fun getAmount(): Long

    /**
     * Returns the maximum amount for the given stack (which may not necessarily
     * be the same as the stack returned by [getItemStack]).
     */
    fun getMaxAmount(stack: ItemStack): Long

    fun set(stack: ItemStack?, amount: Long)

    fun canSet(stack: ItemStack?, amount: Long): Boolean = true
}