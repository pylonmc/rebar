package io.github.pylonmc.rebar.advancements.trigger.base

import io.github.pylonmc.rebar.advancements.trigger.AdvancementTrigger
import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent

class UnlockOnBlockPlaceTrigger(key: NamespacedKey) : AdvancementTrigger<UnlockOnBlockPlaceTriggerInstance>(key), Listener {
    override fun createTriggerInstance(
        advancementKey: NamespacedKey,
        criteriaKey: String,
        options: ConfigSection
    ): UnlockOnBlockPlaceTriggerInstance {
        return UnlockOnBlockPlaceTriggerInstance(
            key,
            advancementKey,
            criteriaKey,
            options.getOrThrow("block", ConfigAdapter.MULTIBLOCK_COMPONENT)
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlaceBlock(event: BlockPlaceEvent){
        instances.filter { it.block.matches(event.block) }
            .forEach { it.trigger(event.player) }
    }
}
