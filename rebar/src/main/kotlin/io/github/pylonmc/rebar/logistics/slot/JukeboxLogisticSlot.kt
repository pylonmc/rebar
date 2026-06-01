package io.github.pylonmc.rebar.logistics.slot

import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.block.Block
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class JukeboxLogisticSlot(block: Block, inventory: Inventory, slot: Int) : VanillaInventoryLogisticSlot(block, inventory, slot) {
    override fun getMaxAmount(stack: ItemStack): Long
        = if (stack.hasData(DataComponentTypes.JUKEBOX_PLAYABLE)) stack.maxStackSize.toLong() else 0L
}