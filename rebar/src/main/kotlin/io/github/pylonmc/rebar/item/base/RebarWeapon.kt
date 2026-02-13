package io.github.pylonmc.rebar.item.base

import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import io.github.pylonmc.rebar.item.research.Research.Companion.canUse
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.entity.Player

interface RebarWeapon {
    /**
     * Called when the item is used to damage an entity.
     */
    fun onUsedToDamageEntity(event: EntityDamageByEntityEvent, priority: EventPriority) {}

    /**
     * Called when the item is used to kill an entity.
     */
    fun onUsedToKillEntity(event: EntityDeathEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onUsedToDamageEntity(event: EntityDamageByEntityEvent, priority: EventPriority) {
            val damager = event.damageSource.causingEntity
            if (event.damageSource.isIndirect || damager !is Player) return

            val rebarItemMainHand = RebarItem.fromStack(damager.inventory.itemInMainHand)
            if (rebarItemMainHand is RebarWeapon) {
                if (!damager.canUse(rebarItemMainHand, false)) {
                    event.isCancelled = true
                    return
                }

                try {
                    MultiHandler.handleEvent(rebarItemMainHand, "onUsedToDamageEntity", event, priority)
                } catch (e: Exception) {
                    RebarItemListener.logEventHandleErr(event, e, rebarItemMainHand)
                }
            }

            val rebarItemOffHand = RebarItem.fromStack(damager.inventory.itemInOffHand)
            if (rebarItemOffHand is RebarWeapon) {
                if (!damager.canUse(rebarItemOffHand, false)) {
                    event.isCancelled = true
                    return
                }

                try {
                    MultiHandler.handleEvent(rebarItemOffHand, "onUsedToDamageEntity", event, priority)
                } catch (e: Exception) {
                    RebarItemListener.logEventHandleErr(event, e, rebarItemOffHand)
                }
            }
        }

        @UniversalHandler
        private fun onUsedToKillEntity(event: EntityDeathEvent, priority: EventPriority) {
            val killer = event.damageSource.causingEntity
            if (killer !is Player) return

            val rebarItemMainHand = RebarItem.fromStack(killer.inventory.itemInMainHand)
            if (rebarItemMainHand is RebarWeapon) {
                if (!killer.canUse(rebarItemMainHand, false)) {
                    event.isCancelled = true
                    return
                }

                try {
                    MultiHandler.handleEvent(rebarItemMainHand, "onUsedToKillEntity", event, priority)
                } catch (e: Exception) {
                    RebarItemListener.logEventHandleErr(event, e, rebarItemMainHand)
                }
            }

            val rebarItemOffHand = RebarItem.fromStack(killer.inventory.itemInOffHand)
            if (rebarItemOffHand is RebarWeapon) {
                if (!killer.canUse(rebarItemOffHand, false)) {
                    event.isCancelled = true
                    return
                }

                try {
                    MultiHandler.handleEvent(rebarItemOffHand, "onUsedToKillEntity", event, priority)
                } catch (e: Exception) {
                    RebarItemListener.logEventHandleErr(event, e, rebarItemOffHand)
                }
            }
        }
    }
}