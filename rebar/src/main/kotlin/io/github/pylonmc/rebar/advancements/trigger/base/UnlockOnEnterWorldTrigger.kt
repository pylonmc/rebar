package io.github.pylonmc.rebar.advancements.trigger.base

import io.github.pylonmc.rebar.advancements.trigger.AdvancementTrigger
import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent

class UnlockOnEnterWorldTrigger(key: NamespacedKey) : AdvancementTrigger<UnlockOnEnterWorldTriggerInstance>(key),
    Listener {
    override fun createTriggerInstance(
        advancementKey: NamespacedKey,
        criteriaKey: String,
        options: ConfigSection
    ): UnlockOnEnterWorldTriggerInstance {
        return UnlockOnEnterWorldTriggerInstance(
            key, advancementKey, criteriaKey, options.getOrThrow(
                "world-key",
                ConfigAdapter.NAMESPACED_KEY
            )
        )
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onChangeWorld(event: PlayerChangedWorldEvent) {
        for (instance in instances) {
            if (event.player.world.key != instance.worldKey) continue
            instance.trigger(event.player)
        }
    }
}