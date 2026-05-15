package io.github.pylonmc.rebar.entity

import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.util.ConfettiParticle
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.entity.Creeper
import org.bukkit.entity.Entity
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.ExplosionPrimeEvent

object ConfettiCreeperListener : MultiListener {
    val confettiCreeper = rebarKey("confetti_creeper")

    @MultiHandler(priorities = [ EventPriority.LOWEST, EventPriority.MONITOR ], ignoreCancelled = true)
    fun onCreeperExplode(event: ExplosionPrimeEvent, priority: EventPriority) {
        val creeper = event.entity
        if (creeper !is Creeper) return

        val pdc = creeper.persistentDataContainer
        if (priority == EventPriority.LOWEST && Math.random() < RebarConfig.ConfettiCreeperConfig.CHANCE) {
            pdc.set(confettiCreeper, RebarSerializers.DOUBLE, event.radius.toDouble())
            event.radius = 0.0F
        } else if (priority == EventPriority.MONITOR && pdc.has(confettiCreeper, RebarSerializers.DOUBLE)) {
            val radius = pdc.getOrDefault(confettiCreeper, RebarSerializers.DOUBLE, 3.0)
            ConfettiParticle.spawnMany(
                creeper.location.add(0.0, creeper.height / 2, 0.0),
                RebarConfig.ConfettiCreeperConfig.AMOUNT,
                radius * 0.25,
                radius * 0.75,
                RebarConfig.ConfettiCreeperConfig.LIFETIME,
            ).get()
        }
    }

    fun isConfettiCreeper(entity: Entity): Boolean {
        return entity is Creeper && entity.persistentDataContainer.has(confettiCreeper, RebarSerializers.DOUBLE)
    }
}