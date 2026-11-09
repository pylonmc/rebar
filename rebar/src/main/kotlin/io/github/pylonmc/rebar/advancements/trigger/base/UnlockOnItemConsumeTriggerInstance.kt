package io.github.pylonmc.rebar.advancements.trigger.base

import io.github.pylonmc.rebar.advancements.trigger.AdvancementTriggerInstance
import io.github.pylonmc.rebar.recipe.ingredient.ItemChoice
import org.bukkit.NamespacedKey

class UnlockOnItemConsumeTriggerInstance(
    triggerKey: NamespacedKey,
    advancementKey: NamespacedKey,
    criteriaKey: String,
    val itemChoice: ItemChoice
) : AdvancementTriggerInstance(triggerKey, advancementKey, criteriaKey)