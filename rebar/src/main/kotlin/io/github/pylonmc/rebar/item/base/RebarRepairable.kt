package io.github.pylonmc.rebar.item.base

import io.github.pylonmc.rebar.item.RebarItemSchema
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack

interface RebarRepairable {
    fun getRepairItems(): List<NamespacedKey>

    fun isValidRepairItem(item: ItemStack): Boolean {
        val key = RebarItemSchema.fromStack(item)?.key ?: item.type.key
        return key in getRepairItems()
    }
}