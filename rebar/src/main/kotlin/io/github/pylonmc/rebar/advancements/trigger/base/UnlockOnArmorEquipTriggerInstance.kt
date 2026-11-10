package io.github.pylonmc.rebar.advancements.trigger.base

import io.github.pylonmc.rebar.advancements.trigger.AdvancementTriggerInstance
import io.github.pylonmc.rebar.recipe.ingredient.ItemChoice
import org.bukkit.NamespacedKey
import org.bukkit.inventory.EquipmentSlot

class UnlockOnArmorEquipTriggerInstance(
    triggerKey: NamespacedKey,
    advancementKey: NamespacedKey,
    criteriaKey: String,
    val armorItem: ItemChoice,
    val slot: EquipmentSlot
) : AdvancementTriggerInstance(triggerKey, advancementKey, criteriaKey)