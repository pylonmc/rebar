package io.github.pylonmc.rebar.advancements.trigger.base

import io.github.pylonmc.rebar.advancements.trigger.AdvancementTrigger
import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent

class UnlockOnEntityKillTrigger(key: NamespacedKey) : AdvancementTrigger<UnlockOnEntityKillTriggerInstance>(key), Listener {
    override fun createTriggerInstance(
        advancementKey: NamespacedKey,
        criteriaKey: String,
        options: ConfigSection
    ): UnlockOnEntityKillTriggerInstance {
        return UnlockOnEntityKillTriggerInstance(
            key,
            advancementKey,
            criteriaKey,
            options.getOrThrow("entity-type-key", ConfigAdapter.NAMESPACED_KEY)
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityKill(event: EntityDeathEvent){
        val killer = event.entity.killer ?: return
        instances.filter { it.entityTypeKey == event.entityType.key }
            .forEach { it.trigger(killer) }
    }
}