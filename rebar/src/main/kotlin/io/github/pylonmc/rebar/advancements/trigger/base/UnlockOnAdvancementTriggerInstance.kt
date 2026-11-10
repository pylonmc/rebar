package io.github.pylonmc.rebar.advancements.trigger.base

import io.github.pylonmc.rebar.advancements.trigger.AdvancementTriggerInstance
import org.bukkit.NamespacedKey

class UnlockOnAdvancementTriggerInstance(
    triggerKey: NamespacedKey,
    advancementKey: NamespacedKey,
    criteriaKey: String,
    val unlockingAdvancementKey: NamespacedKey
) : AdvancementTriggerInstance(triggerKey, advancementKey, criteriaKey) {
}