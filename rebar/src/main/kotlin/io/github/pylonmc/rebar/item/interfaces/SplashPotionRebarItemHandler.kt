package io.github.pylonmc.rebar.item.interfaces

import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.PotionSplashEvent
import org.jetbrains.annotations.ApiStatus

interface SplashPotionRebarItemHandler : ProjectileRebarItemHandler {
    /**
     * Called when the potion hits the ground and 'splashes.'
     */
    fun onPotionSplash(event: PotionSplashEvent, priority: EventPriority)

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onPotionSplash(event: PotionSplashEvent, priority: EventPriority) {
            val rebarPotion = RebarItem.fromStack(event.potion.item, SplashPotionRebarItemHandler::class.java)
            if (rebarPotion is RebarItem) {
                try {
                    MultiHandlers.handleEvent(rebarPotion, "onPotionSplash", event, priority)
                } catch (e: Exception) {
                    RebarItemListener.logEventHandleErr(event, e, rebarPotion)
                }
            }
        }
    }
}