package io.github.pylonmc.rebar.logistics.slot

import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class BrewingStandFuelLogisticSlot(block: Block, inventory: Inventory, slot: Int) : VanillaInventoryLogisticSlot(block, inventory, slot) {
    override fun getMaxAmount(stack: ItemStack): Long
        = if (Tag.ITEMS_BREWING_FUEL.values.contains(stack.type)) stack.maxStackSize.toLong() else 0L
}