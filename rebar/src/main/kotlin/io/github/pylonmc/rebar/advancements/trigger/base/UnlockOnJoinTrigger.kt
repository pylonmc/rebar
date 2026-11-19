package io.github.pylonmc.rebar.advancements.trigger.base

import io.github.pylonmc.rebar.advancements.trigger.AdvancementTrigger
import io.github.pylonmc.rebar.advancements.trigger.AdvancementTriggerInstance
import io.github.pylonmc.rebar.config.ConfigSection
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class UnlockOnJoinTrigger(key: NamespacedKey) : AdvancementTrigger<AdvancementTriggerInstance>(key), Listener {
    override fun createTriggerInstance(
        advancementKey: NamespacedKey,
        criteriaKey: String,
        options: ConfigSection
    ): AdvancementTriggerInstance {
        return AdvancementTriggerInstance(key, advancementKey, criteriaKey)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        for (instance in instances) {
            instance.trigger(event.player)
        }
    }
}
