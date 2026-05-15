package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.logistics.LogisticGroup
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.inventory.InventoryOpenEvent

/**
 * A [RebarVanillaContainerBlock] that cancels all inventory open and move events, effectively preventing any vanilla interaction with the container.
 * Also prevents [LogisticGroup.getVanillaLogisticSlots] from working on the block.
 */
interface RebarNoVanillaContainerBlock : RebarVanillaContainerBlock {
    override fun onInventoryOpen(event: InventoryOpenEvent, priority: EventPriority) = event.run { isCancelled = true }
    override fun onItemMoveTo(event: InventoryMoveItemEvent, priority: EventPriority) = event.run { isCancelled = true }
    override fun onItemMoveFrom(event: InventoryMoveItemEvent, priority: EventPriority) = event.run { isCancelled = true }
}