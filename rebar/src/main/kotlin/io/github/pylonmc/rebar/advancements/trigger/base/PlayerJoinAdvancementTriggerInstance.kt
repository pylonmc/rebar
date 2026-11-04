package io.github.pylonmc.rebar.advancements.trigger.base

import io.github.pylonmc.rebar.advancements.trigger.AdvancementTriggerInstance
import org.bukkit.NamespacedKey

class PlayerJoinAdvancementTriggerInstance(triggerKey: NamespacedKey,
                                           advancementKey: NamespacedKey
) : AdvancementTriggerInstance(triggerKey, advancementKey) {
    // TODO register event handler and handle trigger firing
}