package io.github.pylonmc.rebar.advancements.trigger.base

import io.github.pylonmc.rebar.advancements.trigger.AdvancementTriggerInstance
import org.bukkit.NamespacedKey

class UnlockOnEntityKillTriggerInstance(
    triggerKey: NamespacedKey,
    advancementKey: NamespacedKey,
    criteriaKey: String,
    val entityTypeKey: NamespacedKey
) : AdvancementTriggerInstance(triggerKey, advancementKey, criteriaKey)