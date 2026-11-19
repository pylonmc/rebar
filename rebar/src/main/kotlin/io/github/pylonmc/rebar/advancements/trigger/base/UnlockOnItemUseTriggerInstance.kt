package io.github.pylonmc.rebar.advancements.trigger.base

import io.github.pylonmc.rebar.advancements.trigger.AdvancementTriggerInstance
import io.github.pylonmc.rebar.recipe.ingredient.ItemChoice
import org.bukkit.NamespacedKey
import org.bukkit.event.block.Action

class UnlockOnItemUseTriggerInstance(
    triggerKey: NamespacedKey,
    advancementKey: NamespacedKey,
    criteriaKey: String,
    val item: ItemChoice,
    val actions: Set<Action>
) : AdvancementTriggerInstance(triggerKey, advancementKey, criteriaKey)