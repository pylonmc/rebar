package io.github.pylonmc.rebar.advancements.trigger.base

import io.github.pylonmc.rebar.advancements.trigger.AdvancementTriggerInstance
import org.bukkit.NamespacedKey

class UnlockOnEnterWorldTriggerInstance(
    triggerKey: NamespacedKey,
    advancementKey: NamespacedKey, criteriaKey: String,
    val worldKey: NamespacedKey
) : AdvancementTriggerInstance(triggerKey, advancementKey, criteriaKey) {
}