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
import org.bukkit.inventory.EquipmentSlot

class UnlockOnBlockInteractTrigger(key: NamespacedKey) : AdvancementTrigger<UnlockOnBlockInteractTriggerInstance>(key),
    Listener {
    override fun createTriggerInstance(
        advancementKey: NamespacedKey,
        criteriaKey: String,
        options: ConfigSection
    ): UnlockOnBlockInteractTriggerInstance {
        return UnlockOnBlockInteractTriggerInstance (
            key,
            advancementKey,
            criteriaKey,
            options.getOrThrow("block", ConfigAdapter.MULTIBLOCK_COMPONENT),
            options.getOrThrow("actions", ConfigAdapter.SET.from(ConfigAdapter.ENUM.from<Action>()))
        )
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        if(event.hand != EquipmentSlot.HAND) return
        if(!event.hasBlock()) return
        instances.filter { it.actions.contains(event.action) }
            .filter { it.block.matches(event.clickedBlock!!) }
            .forEach { it.trigger(event.player) }
    }
}