package io.github.pylonmc.rebar.item.base

import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import org.bukkit.entity.ThrowableProjectile
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.jetbrains.annotations.ApiStatus

interface RebarProjectileItem {
    fun onLaunched(event: ProjectileLaunchEvent, priority: EventPriority)
    fun onHit(event: ProjectileHitEvent, priority: EventPriority)

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onProjectileLaunch(event: ProjectileLaunchEvent, priority: EventPriority) {
            val thrownProjectile = event.entity as? ThrowableProjectile ?: return
            val rebarItem = RebarItem.fromStack(thrownProjectile.item)
            if (rebarItem !is RebarProjectileItem) return

            try {
                MultiHandlers.handleEvent(rebarItem, "onLaunched", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }

        @UniversalHandler
        private fun onProjectileHit(event: ProjectileHitEvent, priority: EventPriority) {
            val thrownProjectile = event.entity as? ThrowableProjectile ?: return
            val rebarItem = RebarItem.fromStack(thrownProjectile.item)
            if (rebarItem !is RebarProjectileItem) return

            try {
                MultiHandlers.handleEvent(rebarItem, "onHit", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }
    }
}