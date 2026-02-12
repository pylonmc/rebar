package io.github.pylonmc.rebar.item.base

import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener.logEventHandleErr
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.PotionSplashEvent

interface RebarSplashPotion {
    /**
     * Called when the potion hits the ground and 'splashes.'
     */
    fun onSplash(event: PotionSplashEvent, priority: EventPriority)

    companion object : MultiListener {
        @UniversalHandler
        private fun handle(event: PotionSplashEvent, priority: EventPriority) {
            val rebarPotion = RebarItem.fromStack(event.potion.item)
            if (rebarPotion is RebarSplashPotion) {
                try {
                    MultiHandler.handleEvent(rebarPotion, "onSplash", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarPotion)
                }
            }
        }
    }
}