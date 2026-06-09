package io.github.pylonmc.rebar.recipe.slot.item.implementation

import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class BrewingStandFuelItemSlot(block: Block, inventory: Inventory, slot: Int) : VanillaInventoryItemSlot(block, inventory, slot) {
    override fun getMaxAmount(stack: ItemStack): Long
        = if (Tag.ITEMS_BREWING_FUEL.values.contains(stack.type)) stack.maxStackSize.toLong() else 0L
}