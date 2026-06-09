package io.github.pylonmc.rebar.recipe.slot.item.implementation

import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.block.Block
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class BrewingStandPotionItemSlot(block: Block, inventory: Inventory, slot: Int) : VanillaInventoryItemSlot(block, inventory, slot) {
    override fun getMaxAmount(stack: ItemStack): Long
        // close enough lol
        = if (stack.hasData(DataComponentTypes.POTION_CONTENTS)) stack.maxStackSize.toLong() else 0L
}