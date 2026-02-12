package io.github.pylonmc.rebar.item.base

import org.bukkit.event.EventPriority
import org.bukkit.event.entity.PotionSplashEvent

interface RebarSplashPotion {
    /**
     * Called when the potion hits the ground and 'splashes.'
     */
    fun onSplash(event: PotionSplashEvent, priority: EventPriority)
}