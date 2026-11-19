package io.github.pylonmc.rebar.advancements.trigger.base

import io.github.pylonmc.rebar.advancements.trigger.AdvancementTrigger
import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent

class UnlockOnItemConsumeTrigger(key: NamespacedKey) : AdvancementTrigger<UnlockOnItemConsumeTriggerInstance>(key),
    Listener {
    override fun createTriggerInstance(
        advancementKey: NamespacedKey,
        criteriaKey: String,
        options: ConfigSection
    ): UnlockOnItemConsumeTriggerInstance {
        return UnlockOnItemConsumeTriggerInstance(
            key, advancementKey, criteriaKey,
            options.getOrThrow("item", ConfigAdapter.ITEM_CHOICE)
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun handlePlayerConsumeItem(event: PlayerItemConsumeEvent) {
        for (trigger in instances) {
            if (trigger.itemChoice.matches(event.item)) {
                trigger.trigger(event.player)
            }
        }
    }
}