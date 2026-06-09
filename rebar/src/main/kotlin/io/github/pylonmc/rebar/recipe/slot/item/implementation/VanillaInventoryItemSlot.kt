package io.github.pylonmc.rebar.recipe.slot.item.implementation

import io.github.pylonmc.rebar.nms.NmsAccessor
import io.github.pylonmc.rebar.recipe.slot.item.ItemSlot
import org.bukkit.block.Block
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

open class VanillaInventoryItemSlot(val block: Block, val inventory: Inventory, val slot: Int) : ItemSlot {
    override fun getItemStack(): ItemStack? = inventory.getItem(slot)
    override fun getAmount(): Long = getItemStack()?.amount?.toLong() ?: 0
    override fun getMaxAmount(stack: ItemStack): Long = stack.maxStackSize.toLong()
    override fun set(stack: ItemStack?, amount: Long) {
        inventory.setItem(slot, stack?.asQuantity(amount.toInt()))
        NmsAccessor.instance.setChanged(inventory)
    }
}