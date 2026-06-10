package io.github.pylonmc.rebar.logistics.slot

import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack

open class ItemDisplayLogisticSlot(val display: ItemDisplay) : LogisticSlot {
    override fun getItemStack(): ItemStack? = display.itemStack
    override fun getAmount(): Long = getItemStack()?.amount?.toLong() ?: 0
    override fun getMaxAmount(stack: ItemStack): Long = stack.maxStackSize.toLong()
    override fun set(stack: ItemStack?, amount: Long) = display.setItemStack(stack?.asQuantity(amount.toInt()))
}