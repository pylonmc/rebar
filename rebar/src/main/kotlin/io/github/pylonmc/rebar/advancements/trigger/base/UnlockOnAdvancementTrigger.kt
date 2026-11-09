package io.github.pylonmc.rebar.advancements.trigger.base

import io.github.pylonmc.rebar.advancements.trigger.AdvancementTrigger
import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerAdvancementDoneEvent

class UnlockOnAdvancementTrigger(key: NamespacedKey) : AdvancementTrigger<UnlockOnAdvancementTriggerInstance>(key),
    Listener {
    override fun createTriggerInstance(
        advancementKey: NamespacedKey,
        criteriaKey: String,
        options: ConfigSection
    ): UnlockOnAdvancementTriggerInstance {
        return UnlockOnAdvancementTriggerInstance(
            key,
            advancementKey,
            criteriaKey,
            options.getOrThrow("advancement-key", ConfigAdapter.NAMESPACED_KEY)
        )
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun handleAdvancementUnlock(event: PlayerAdvancementDoneEvent) {
        for (instance in instances) {
            if (instance.unlockingAdvancementKey != event.advancement.key) continue;
            instance.trigger(event.player)
        }
    }
}