package io.github.pylonmc.rebar.block.base

import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.inventory.InventoryOpenEvent

interface RebarNoVanillaContainerBlock : RebarVanillaContainerBlock {
    override fun onInventoryOpen(event: InventoryOpenEvent, priority: EventPriority) = event.run { isCancelled = true }
    override fun onItemMoveTo(event: InventoryMoveItemEvent, priority: EventPriority) = event.run { isCancelled = true }
    override fun onItemMoveFrom(event: InventoryMoveItemEvent, priority: EventPriority) = event.run { isCancelled = true }
}