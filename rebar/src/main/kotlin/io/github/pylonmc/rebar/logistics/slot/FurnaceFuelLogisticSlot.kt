package io.github.pylonmc.rebar.logistics.slot

import org.bukkit.block.Block
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class FurnaceFuelLogisticSlot(block: Block, inventory: Inventory, slot: Int) : VanillaInventoryLogisticSlot(block, inventory, slot) {
    override fun getMaxAmount(stack: ItemStack): Long
        = if (stack.type.isFuel) stack.maxStackSize.toLong() else 0L
}