package io.github.pylonmc.rebar.logistics.slot

import io.github.pylonmc.rebar.nms.NmsAccessor
import org.bukkit.block.Block
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class BrewingStandFuelLogisticSlot(block: Block, inventory: Inventory, slot: Int) : VanillaInventoryLogisticSlot(block, inventory, slot) {
    override fun getMaxAmount(stack: ItemStack): Long
        = if (NmsAccessor.instance.isBrewingFuel(stack)) stack.maxStackSize.toLong() else 0L
}
