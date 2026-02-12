package io.github.pylonmc.rebar.item.base

import org.bukkit.event.EventPriority
import org.bukkit.event.entity.LingeringPotionSplashEvent

interface RebarLingeringPotion {
    /**
     * Called when the potion hits the ground and 'splashes.'
     */
    fun onSplash(event: LingeringPotionSplashEvent, priority: EventPriority)
}