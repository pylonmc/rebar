package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.item.RebarItem
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

/**
 * Represents a block that takes the form of another block when right clicked, such as fluid
 * pipes and cargo ducts.
 */
interface RebarFacadeBlock : RebarInteractBlock {

    /**
     * Implemented automatically by any class that extends PylonBlock
     */
    val block: Block

    val facadeDefaultBlockType: Material

    @MultiHandler(priorities = [EventPriority.MONITOR])
    override fun onInteract(event: PlayerInteractEvent, priority: EventPriority) {
        if (!event.action.isRightClick || event.player.isSneaking || event.hand != EquipmentSlot.HAND) {
            return
        }

        val item = event.player.inventory.getItem(EquipmentSlot.HAND)
        if (RebarItem.isRebarItem(item)) {
            return
        }

        if (block.type != Material.STRUCTURE_VOID) {
            block.type = Material.STRUCTURE_VOID
            event.setUseItemInHand(Event.Result.DENY)
            event.setUseInteractedBlock(Event.Result.DENY)
            return
        }

        if (item.type.isBlock && !item.type.isAir) {
            block.type = item.type
            event.setUseItemInHand(Event.Result.DENY)
            event.setUseInteractedBlock(Event.Result.DENY)
        }
    }
}