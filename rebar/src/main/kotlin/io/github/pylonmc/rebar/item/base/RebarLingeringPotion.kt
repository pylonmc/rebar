package io.github.pylonmc.rebar.item.base

import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener.logEventHandleErr
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.LingeringPotionSplashEvent
import org.jetbrains.annotations.ApiStatus

interface RebarLingeringPotion {
    /**
     * Called when the potion hits the ground and 'splashes.'
     */
    fun onSplash(event: LingeringPotionSplashEvent, priority: EventPriority)

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun handle(event: LingeringPotionSplashEvent, priority: EventPriority) {
            val rebarPotion = RebarItem.fromStack(event.entity.item, RebarLingeringPotion::class.java)
            if (rebarPotion is RebarItem) {
                try {
                    MultiHandlers.handleEvent(rebarPotion, "onSplash", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarPotion)
                }
            }
        }
    }
}