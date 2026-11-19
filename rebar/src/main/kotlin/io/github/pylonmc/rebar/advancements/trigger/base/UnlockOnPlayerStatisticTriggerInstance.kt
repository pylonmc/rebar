package io.github.pylonmc.rebar.advancements.trigger.base

import io.github.pylonmc.rebar.advancements.trigger.AdvancementTriggerInstance
import org.bukkit.NamespacedKey
import org.bukkit.Statistic

class UnlockOnPlayerStatisticTriggerInstance(
    triggerKey: NamespacedKey,
    advancementKey: NamespacedKey,
    criteriaKey: String,
    val statistic: Statistic,
    val value: Int
) : AdvancementTriggerInstance(triggerKey, advancementKey, criteriaKey)