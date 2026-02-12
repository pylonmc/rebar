package io.github.pylonmc.rebar.item

import com.destroystokyo.paper.event.player.PlayerReadyArrowEvent
import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.item.base.*
import io.github.pylonmc.rebar.item.research.Research.Companion.canUse
import io.github.pylonmc.rebar.util.findRebarItemInInventory
import io.papermc.paper.event.player.PlayerPickItemEvent
import org.bukkit.GameMode
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.block.BlockDispenseEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.inventory.BrewingStandFuelEvent
import org.bukkit.event.inventory.FurnaceBurnEvent
import org.bukkit.event.player.*
import kotlin.math.ceil

internal object RebarItemListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: PlayerReadyArrowEvent) {
        val bow = RebarItem.fromStack(event.bow)
        if (bow != null && !event.player.canUse(bow, true)) {
            event.isCancelled = true
            return
        }

        val arrow = RebarItem.fromStack(event.arrow)
        if (arrow != null && !event.player.canUse(arrow, true)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: PlayerInteractEvent) {
        val rebarItem = event.item?.let { RebarItem.fromStack(it) } ?: return
        if (!event.player.canUse(rebarItem, true)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: PlayerBucketEmptyEvent) {
        val rebarItem = event.itemStack?.let { RebarItem.fromStack(it) }
        if (rebarItem != null && !event.player.canUse(rebarItem, true)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: PlayerBucketFillEvent) {
        val stack = event.player.inventory.getItem(event.hand)
        val rebarItem = RebarItem.fromStack(stack)
        if (rebarItem != null && !event.player.canUse(rebarItem, true)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: PlayerItemConsumeEvent) {
        val rebarItem = RebarItem.fromStack(event.item)
        if (rebarItem != null && !event.player.canUse(rebarItem, true)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: PlayerItemDamageEvent) {
        val rebarItem = RebarItem.fromStack(event.item)
        if (rebarItem != null) {
            event.player.canUse(rebarItem, true)
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: PlayerItemBreakEvent) {
        val rebarItem = RebarItem.fromStack(event.brokenItem)
        if (rebarItem != null) {
            event.player.canUse(rebarItem, true)
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: PlayerItemMendEvent) {
        val rebarItem = RebarItem.fromStack(event.item)
        if (rebarItem != null && !event.player.canUse(rebarItem, true)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: PlayerInteractEntityEvent) {
        val rebarItemMainHand = RebarItem.fromStack(event.player.inventory.itemInMainHand)
        if (rebarItemMainHand != null && !event.player.canUse(rebarItemMainHand, true)) {
            event.isCancelled = true
            return
        }

        val rebarItemOffHand = RebarItem.fromStack(event.player.inventory.itemInOffHand)
        if (rebarItemOffHand != null && !event.player.canUse(rebarItemOffHand, true)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: BlockDamageEvent) {
        val rebarItem = RebarItem.fromStack(event.itemInHand)
        if (rebarItem != null && !event.player.canUse(rebarItem, true)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: BlockBreakEvent) {
        val rebarItemMainHand = RebarItem.fromStack(event.player.inventory.itemInMainHand)
        if (rebarItemMainHand != null && !event.player.canUse(rebarItemMainHand, true)) {
            event.isCancelled = true
            return
        }

        val rebarItemOffHand = RebarItem.fromStack(event.player.inventory.itemInOffHand)
        if (rebarItemOffHand != null && !event.player.canUse(rebarItemOffHand, true)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: EntityDamageByEntityEvent) {
        val damager = event.damageSource.causingEntity
        if (event.damageSource.isIndirect || damager !is Player) return

        val rebarItemMainHand = RebarItem.fromStack(damager.inventory.itemInMainHand)
        if (rebarItemMainHand != null && !damager.canUse(rebarItemMainHand, true)) {
            event.isCancelled = true
            return
        }

        val rebarItemOffHand = RebarItem.fromStack(damager.inventory.itemInOffHand)
        if (rebarItemOffHand != null && !damager.canUse(rebarItemOffHand, true)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: EntityDeathEvent) {
        val killer = event.damageSource.causingEntity
        if (killer !is Player) return
        val rebarItemMainHand = RebarItem.fromStack(killer.inventory.itemInMainHand)
        if (rebarItemMainHand != null && !killer.canUse(rebarItemMainHand, true)) {
            event.isCancelled = true
            return
        }

        val rebarItemOffHand = RebarItem.fromStack(killer.inventory.itemInOffHand)
        if (rebarItemOffHand != null && !killer.canUse(rebarItemOffHand, true)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    private fun handle(event: PlayerPickItemEvent) {
        val reachDistance = event.player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE)?.value ?: 4.5
        val block = event.player.getTargetBlockExact(ceil(reachDistance).toInt()) ?: return
        val rebarBlock = BlockStorage.get(block) ?: return
        val blockItem = rebarBlock.getPickItem() ?: return
        val blockRebarItem = RebarItem.fromStack(blockItem) ?: return

        val sourceSlot = event.sourceSlot
        if (sourceSlot != -1) {
            val sourceItem = event.player.inventory.getItem(event.sourceSlot)
            if (sourceItem != null) {
                val sourceRebarItem = RebarItem.fromStack(sourceItem)
                if (sourceRebarItem != null) {
                    // The source item is already of the correct Rebar type, so we shouldn't interfere with the event
                    return
                }
            }
        }

        // If we reach this point, the source item is not of the correct type
        // So we're going to search the inventory for a block of the correct type
        val existingSlot = findRebarItemInInventory(event.player.inventory, blockRebarItem)
        if (existingSlot != null) {
            // If we find one, we'll set the source to that slot
            event.sourceSlot = existingSlot
            // And if the item is in the hotbar, that should become the target (0-8 are hotbar slots
            if (existingSlot <= 8) {
                event.targetSlot = existingSlot
            }
            return
        }

        // Otherwise, we'll just attempt to add a new item and set the source to be that item
        if (event.player.gameMode == GameMode.CREATIVE) {
            if (event.player.inventory.addItem(blockItem).isNotEmpty()) {
                // Inventory full, can't pick the item
                event.isCancelled = true
                return
            }
        }

        val newSourceSlot = findRebarItemInInventory(event.player.inventory, blockRebarItem)
        if (newSourceSlot == null) {
            // should never happen but you never know
            event.isCancelled = true
            return
        }

        event.sourceSlot = newSourceSlot
        event.targetSlot = event.player.inventory.heldItemSlot

        // don't question this idk wtf is going on - seems we have to manually do the swap in the hotbar
        if (sourceSlot <= 8) {
            val source = event.player.inventory.getItem(event.sourceSlot)
            val target = event.player.inventory.getItem(event.targetSlot)
            event.player.inventory.setItem(event.sourceSlot, target)
            event.player.inventory.setItem(event.targetSlot, source)
        }
    }

    @JvmSynthetic
    internal fun logEventHandleErr(event: Event, e: Exception, item: RebarItem) {
        Rebar.logger.severe("Error when handling item(${item.key}) event handler ${event.javaClass.simpleName}: ${e.localizedMessage}")
        e.printStackTrace()
    }
}