package io.github.pylonmc.rebar.logistics.slot

import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.block.Block
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class BrewingStandPotionLogisticSlot(block: Block, inventory: Inventory, slot: Int) : VanillaInventoryLogisticSlot(block, inventory, slot) {
    override fun getMaxAmount(stack: ItemStack): Long
        // close enough lol
        = if (stack.hasData(DataComponentTypes.POTION_CONTENTS)) stack.maxStackSize.toLong() else 0L
}