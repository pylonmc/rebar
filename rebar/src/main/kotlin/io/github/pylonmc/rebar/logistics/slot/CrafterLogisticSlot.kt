package io.github.pylonmc.rebar.logistics.slot

import org.bukkit.block.Block
import org.bukkit.block.Crafter
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class CrafterLogisticSlot(val block: Block, inventory: Inventory, slot: Int) : VanillaInventoryLogisticSlot(inventory, slot) {
    override fun canSet(stack: ItemStack?, amount: Long): Boolean {
        val crafter = block.state as? Crafter ?: return false
        return !crafter.isSlotDisabled(slot)
    }
}