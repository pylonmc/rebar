package io.github.pylonmc.rebar.item.interfaces

import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import io.github.pylonmc.rebar.item.research.Research.Companion.canUse
import io.github.pylonmc.rebar.util.getWeaponItem
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.jetbrains.annotations.ApiStatus

interface EntityAttackRebarItemHandler {

    /**
     * Called when the item is used to attempt to attack an entity
     */
    fun onPreAttack(event: PrePlayerAttackEntityEvent, priority: EventPriority) {}

    /**
     * Called when the item is used to damage an entity.
     */
    fun onDamageEntity(event: EntityDamageByEntityEvent, priority: EventPriority) {}

    /**
     * Called when the item is used to kill an entity.
     */
    fun onKillEntity(event: EntityDeathEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onPreAttack(event: PrePlayerAttackEntityEvent, priority: EventPriority) {
            val player = event.player
            val rebarItem = RebarItem.fromStack(player.getWeaponItem(), EntityAttackRebarItemHandler::class.java)
            if (rebarItem !is RebarItem) return

            if (!player.canUse(rebarItem, false)) {
                event.isCancelled = true
                return
            }

            try {
                MultiHandlers.handleEvent(rebarItem, "onPreAttack", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }

        @UniversalHandler
        private fun onDamageEntity(event: EntityDamageByEntityEvent, priority: EventPriority) {
            val damager = event.damageSource.causingEntity
            if (damager == null || event.damageSource.isIndirect) return

            val rebarItem = RebarItem.fromStack(damager.getWeaponItem(), EntityAttackRebarItemHandler::class.java)
            if (rebarItem !is RebarItem) return

            if (damager is Player && !damager.canUse(rebarItem, false)) {
                event.isCancelled = true
                return
            }

            try {
                MultiHandlers.handleEvent(rebarItem, "onDamageEntity", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }

        @UniversalHandler
        private fun onKillEntity(event: EntityDeathEvent, priority: EventPriority) {
            val killer = event.damageSource.causingEntity
            if (killer == null || event.damageSource.isIndirect) return

            val rebarItem = RebarItem.fromStack(killer.getWeaponItem(), EntityAttackRebarItemHandler::class.java)
            if (rebarItem !is RebarItem) return

            if (killer is Player && !killer.canUse(rebarItem, false)) {
                event.isCancelled = true
                return
            }

            try {
                MultiHandlers.handleEvent(rebarItem, "onKillEntity", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }
    }
}