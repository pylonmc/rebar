package io.github.pylonmc.rebar.advancements.trigger.base

import io.github.pylonmc.rebar.advancements.trigger.AdvancementTrigger
import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemBreakEvent

class UnlockOnItemBreakTrigger(key: NamespacedKey) : AdvancementTrigger<UnlockOnItemBreakTriggerInstance>(key),
    Listener {
    override fun createTriggerInstance(
        advancementKey: NamespacedKey,
        criteriaKey: String,
        options: ConfigSection
    ): UnlockOnItemBreakTriggerInstance {
        return UnlockOnItemBreakTriggerInstance(
            key,
            advancementKey,
            criteriaKey,
            options.getOrThrow("item", ConfigAdapter.ITEM_CHOICE)
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onItemBreak(event: PlayerItemBreakEvent) {
        instances.filter { it.itemChoice.matches(event.brokenItem) }
            .forEach { it.trigger(event.player) }
    }
}