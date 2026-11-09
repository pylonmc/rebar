package io.github.pylonmc.rebar.advancements.trigger.base

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent
import io.github.pylonmc.rebar.advancements.trigger.AdvancementTrigger
import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.inventory.EquipmentSlot

class UnlockOnArmorEquipTrigger(key: NamespacedKey) : AdvancementTrigger<UnlockOnArmorEquipTriggerInstance>(key),
    Listener {
    override fun createTriggerInstance(
        advancementKey: NamespacedKey,
        criteriaKey: String,
        options: ConfigSection
    ): UnlockOnArmorEquipTriggerInstance {
        return UnlockOnArmorEquipTriggerInstance(
            key,
            advancementKey,
            criteriaKey,
            options.getOrThrow("item", ConfigAdapter.ITEM_CHOICE),
            options.getOrThrow("slot", ConfigAdapter.ENUM.from<EquipmentSlot>())
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onArmorChange(event: PlayerArmorChangeEvent) {
        instances.filter { it.armorItem.matches(event.newItem) }
            .forEach {
                it.trigger(event.player)
            }
    }
}