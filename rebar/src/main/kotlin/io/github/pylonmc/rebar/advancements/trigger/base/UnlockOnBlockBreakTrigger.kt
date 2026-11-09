package io.github.pylonmc.rebar.advancements.trigger.base

import io.github.pylonmc.rebar.advancements.trigger.AdvancementTrigger
import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent

class UnlockOnBlockBreakTrigger(key: NamespacedKey) : AdvancementTrigger<UnlockOnBlockBreakTriggerInstance>(key),
    Listener {
    override fun createTriggerInstance(
        advancementKey: NamespacedKey,
        criteriaKey: String,
        options: ConfigSection
    ): UnlockOnBlockBreakTriggerInstance {
        return UnlockOnBlockBreakTriggerInstance(
            key,
            advancementKey,
            criteriaKey,
            options.getOrThrow("block", ConfigAdapter.MULTIBLOCK_COMPONENT)
        )
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBreakBlock(event: BlockBreakEvent) {
        instances.filter { it.block.matches(event.block) }
            .forEach { it.trigger(event.player) }
    }
}