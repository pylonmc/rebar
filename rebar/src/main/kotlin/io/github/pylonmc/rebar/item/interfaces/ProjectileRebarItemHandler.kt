package io.github.pylonmc.rebar.item.interfaces

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.util.sourceItem
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.jetbrains.annotations.ApiStatus

interface ProjectileRebarItemHandler {
    /**
     * Called when the item is launched as a projectile
     */
    fun onProjectileLaunched(event: ProjectileLaunchEvent, priority: EventPriority) {}

    /**
     * Called when the item as a projectile hits something
     */
    fun onProjectileHit(event: ProjectileHitEvent, priority: EventPriority) {}

    /**
     * Called when the item as a projectile damages an entity.
     */
    fun onProjectileDamageEntity(event: EntityDamageByEntityEvent, priority: EventPriority) {}

    /**
     * Called when the item as a projectile kills an entity
     */
    fun onProjectileKillEntity(event: EntityDeathEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        val sourceItemKey = rebarKey("projectile_source_item")

        @EventHandler(priority = EventPriority.LOWEST)
        fun onPlayerLaunched(event: PlayerLaunchProjectileEvent) {
            if (event.projectile.sourceItem() == null) {
                event.projectile.persistentDataContainer.set(sourceItemKey, RebarSerializers.ITEM_STACK, event.itemStack)
            }
        }

        @EventHandler(priority = EventPriority.LOWEST)
        fun onShotFromBow(event: EntityShootBowEvent) {
            val projectile = event.projectile as? Projectile ?: return
            val sourceItem = event.consumable ?: return
            if (projectile.sourceItem() == null) {
                projectile.persistentDataContainer.set(sourceItemKey, RebarSerializers.ITEM_STACK, sourceItem)
            }
        }

        @UniversalHandler
        private fun onProjectileLaunched(event: ProjectileLaunchEvent, priority: EventPriority) {
            val rebarItem = RebarItem.fromStack(event.entity.sourceItem(), ProjectileRebarItemHandler::class.java)
            if (rebarItem !is RebarItem) return

            try {
                MultiHandlers.handleEvent(rebarItem, "onProjectileLaunched", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }

        @UniversalHandler
        private fun onProjectileHit(event: ProjectileHitEvent, priority: EventPriority) {
            val rebarItem = RebarItem.fromStack(event.entity.sourceItem(), ProjectileRebarItemHandler::class.java)
            if (rebarItem !is RebarItem) return

            try {
                MultiHandlers.handleEvent(rebarItem, "onProjectileHit", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }

        @UniversalHandler
        private fun onProjectileDamageEntity(event: EntityDamageByEntityEvent, priority: EventPriority) {
            val damager = event.damageSource.directEntity
            if (damager !is Projectile) return

            val rebarItem = RebarItem.fromStack(damager.sourceItem(), ProjectileRebarItemHandler::class.java)
            if (rebarItem !is RebarItem) return

            try {
                MultiHandlers.handleEvent(rebarItem, "onProjectileDamageEntity", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }

        @UniversalHandler
        private fun onProjectileKillEntity(event: EntityDeathEvent, priority: EventPriority) {
            val damager = event.damageSource.directEntity
            if (damager !is Projectile) return

            val rebarItem = RebarItem.fromStack(damager.sourceItem(), ProjectileRebarItemHandler::class.java)
            if (rebarItem !is RebarItem) return

            try {
                MultiHandlers.handleEvent(rebarItem, "onProjectileKillEntity", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }
    }
}