package io.github.pylonmc.rebar.recipe.slot.item.implementation

import org.bukkit.block.Block
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class FurnaceFuelItemSlot(block: Block, inventory: Inventory, slot: Int) : VanillaInventoryItemSlot(block, inventory, slot) {
    override fun getMaxAmount(stack: ItemStack): Long
        = if (stack.type.isFuel) stack.maxStackSize.toLong() else 0L
}