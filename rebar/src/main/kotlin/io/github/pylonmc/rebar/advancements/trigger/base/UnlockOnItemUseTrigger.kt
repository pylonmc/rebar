package io.github.pylonmc.rebar.advancements.trigger.base

import io.github.pylonmc.rebar.advancements.trigger.AdvancementTrigger
import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

class UnlockOnItemUseTrigger(key: NamespacedKey) : AdvancementTrigger<UnlockOnItemUseTriggerInstance>(key), Listener {
    override fun createTriggerInstance(
        advancementKey: NamespacedKey,
        criteriaKey: String,
        options: ConfigSection
    ): UnlockOnItemUseTriggerInstance {
        return UnlockOnItemUseTriggerInstance(
            key,
            advancementKey,
            criteriaKey,
            options.getOrThrow("item", ConfigAdapter.ITEM_CHOICE),
            options.getOrThrow("actions", ConfigAdapter.SET.from(ConfigAdapter.ENUM.from<Action>()))
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onItemUse(event: PlayerInteractEvent){
        instances.filter { it.actions.contains(event.action) }
            .filter { it.item.matches(event.item) }
            .forEach { it.trigger(event.player) }
    }
}