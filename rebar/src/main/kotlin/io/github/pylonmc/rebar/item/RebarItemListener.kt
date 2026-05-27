package io.github.pylonmc.rebar.item

import com.destroystokyo.paper.event.player.PlayerReadyArrowEvent
import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.item.research.Research.Companion.canUse
import io.github.pylonmc.rebar.util.findRebar
import io.github.pylonmc.rebar.util.findType
import io.papermc.paper.event.player.PlayerPickBlockEvent
import io.papermc.paper.event.player.PlayerPickEntityEvent
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.*

internal object RebarItemListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: PlayerReadyArrowEvent) {
        val bow = RebarItemSchema.fromStack(event.bow)
        if (bow != null && !event.player.canUse(bow, true)) {
            event.isCancelled = true
            return
        }

        val arrow = RebarItemSchema.fromStack(event.arrow)
        if (arrow != null && !event.player.canUse(arrow, true)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: PlayerInteractEvent) {
        val rebarItem = event.item?.let { RebarItemSchema.fromStack(it) } ?: return
        if (!event.player.canUse(rebarItem, true)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: PlayerBucketEmptyEvent) {
        val rebarItem = event.itemStack?.let { RebarItemSchema.fromStack(it) }
        if (rebarItem != null && !event.player.canUse(rebarItem, true)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: PlayerBucketFillEvent) {
        val stack = event.player.inventory.getItem(event.hand)
        val rebarItem = RebarItemSchema.fromStack(stack)
        if (rebarItem != null && !event.player.canUse(rebarItem, true)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: PlayerItemConsumeEvent) {
        val rebarItem = RebarItemSchema.fromStack(event.item)
        if (rebarItem != null && !event.player.canUse(rebarItem, true)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: PlayerItemDamageEvent) {
        val rebarItem = RebarItemSchema.fromStack(event.item)
        if (rebarItem != null) {
            event.player.canUse(rebarItem, true)
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: PlayerItemBreakEvent) {
        val rebarItem = RebarItemSchema.fromStack(event.brokenItem)
        if (rebarItem != null) {
            event.player.canUse(rebarItem, true)
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: PlayerItemMendEvent) {
        val rebarItem = RebarItemSchema.fromStack(event.item)
        if (rebarItem != null && !event.player.canUse(rebarItem, true)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: PlayerInteractEntityEvent) {
        val rebarItemMainHand = RebarItemSchema.fromStack(event.player.inventory.itemInMainHand)
        if (rebarItemMainHand != null && !event.player.canUse(rebarItemMainHand, true)) {
            event.isCancelled = true
            return
        }

        val rebarItemOffHand = RebarItemSchema.fromStack(event.player.inventory.itemInOffHand)
        if (rebarItemOffHand != null && !event.player.canUse(rebarItemOffHand, true)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: BlockDamageEvent) {
        val rebarItem = RebarItemSchema.fromStack(event.itemInHand)
        if (rebarItem != null && !event.player.canUse(rebarItem, true)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: BlockBreakEvent) {
        val rebarItemMainHand = RebarItemSchema.fromStack(event.player.inventory.itemInMainHand)
        if (rebarItemMainHand != null && !event.player.canUse(rebarItemMainHand, true)) {
            event.isCancelled = true
            return
        }

        val rebarItemOffHand = RebarItemSchema.fromStack(event.player.inventory.itemInOffHand)
        if (rebarItemOffHand != null && !event.player.canUse(rebarItemOffHand, true)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: EntityDamageByEntityEvent) {
        val damager = event.damageSource.causingEntity
        if (event.damageSource.isIndirect || damager !is Player) return

        val rebarItemMainHand = RebarItemSchema.fromStack(damager.inventory.itemInMainHand)
        if (rebarItemMainHand != null && !damager.canUse(rebarItemMainHand, true)) {
            event.isCancelled = true
            return
        }

        val rebarItemOffHand = RebarItemSchema.fromStack(damager.inventory.itemInOffHand)
        if (rebarItemOffHand != null && !damager.canUse(rebarItemOffHand, true)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: EntityDeathEvent) {
        val killer = event.damageSource.causingEntity
        if (killer !is Player) return
        val rebarItemMainHand = RebarItemSchema.fromStack(killer.inventory.itemInMainHand)
        if (rebarItemMainHand != null && !killer.canUse(rebarItemMainHand, true)) {
            event.isCancelled = true
            return
        }

        val rebarItemOffHand = RebarItemSchema.fromStack(killer.inventory.itemInOffHand)
        if (rebarItemOffHand != null && !killer.canUse(rebarItemOffHand, true)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: PlayerPickBlockEvent) {
        val rebarBlock = BlockStorage.get(event.block) ?: return
        val blockItem = rebarBlock.getPickItem() ?: return
        val blockSchema = RebarItemSchema.fromStack(blockItem) ?: return

        val sourceSlot = event.sourceSlot
        if (sourceSlot != -1) {
            val sourceItem = event.player.inventory.getItem(event.sourceSlot)
            if (sourceItem != null) {
                val sourceSchema = RebarItemSchema.fromStack(sourceItem)
                if (sourceSchema == blockSchema) {
                    // The source item is already of the correct Rebar type, so we shouldn't interfere with the event
                    return
                }
            }
        }

        // If we reach this point, the source item is not of the correct type
        // So we're going to search the inventory for a block of the correct type
        val existingSlot = event.player.inventory.findRebar(blockSchema)
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

        val newSourceSlot = event.player.inventory.findRebar(blockSchema)
        if (newSourceSlot == null) {
            // should never happen but you never know
            event.isCancelled = true
            return
        }

        event.sourceSlot = newSourceSlot
        event.targetSlot = event.player.inventory.heldItemSlot
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    internal fun handle(event: PlayerPickEntityEvent) {
        val rebarEntity = EntityStorage.get(event.entity) ?: return
        val entityItem = rebarEntity.getPickItem() ?: return
        val entityItemType = ItemTypeWrapper(entityItem)

        val sourceSlot = event.sourceSlot
        if (sourceSlot != -1) {
            val sourceItem = event.player.inventory.getItem(event.sourceSlot)
            if (sourceItem != null && sourceItem.isSimilar(entityItem)) {
                // The source item is already correct
                return
            }
        }

        // If we reach this point, the source item is not of the correct type
        // So we're going to search the inventory for a block of the correct type
        val existingSlot = event.player.inventory.findType(entityItemType)
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
            if (event.player.inventory.addItem(entityItem).isNotEmpty()) {
                // Inventory full, can't pick the item
                event.isCancelled = true
                return
            }
        }

        val newSourceSlot = event.player.inventory.findType(entityItemType)
        if (newSourceSlot == null) {
            // should never happen but you never know
            event.isCancelled = true
            return
        }

        event.sourceSlot = newSourceSlot
        event.targetSlot = event.player.inventory.heldItemSlot
    }

    @JvmSynthetic
    internal fun logEventHandleErr(event: Event, e: Exception, item: RebarItem) {
        Rebar.logger.severe("Error when handling item(${item.key}) event handler ${event.javaClass.simpleName}: ${e.localizedMessage}")
        e.printStackTrace()
    }
}