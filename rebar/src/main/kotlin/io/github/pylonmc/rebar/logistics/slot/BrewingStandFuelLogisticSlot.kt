package io.github.pylonmc.rebar.logistics.slot

import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.block.Block
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class BrewingStandFuelLogisticSlot(block: Block, inventory: Inventory, slot: Int) : VanillaInventoryLogisticSlot(block, inventory, slot) {
    override fun getMaxAmount(stack: ItemStack): Long
        = if (stack.hasData(brewingFuel)) stack.maxStackSize.toLong() else 0L

    companion object {
        private val brewingFuel = Registry.DATA_COMPONENT_TYPE.getOrThrow(NamespacedKey.minecraft("brewing_fuel"))
    }
}
