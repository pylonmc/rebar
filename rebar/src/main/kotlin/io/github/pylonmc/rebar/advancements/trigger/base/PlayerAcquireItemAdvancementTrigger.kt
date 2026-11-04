package io.github.pylonmc.rebar.advancements.trigger.base

import io.github.pylonmc.rebar.advancements.RebarAdvancement
import io.github.pylonmc.rebar.advancements.trigger.AdvancementTrigger
import io.github.pylonmc.rebar.config.ConfigSection
import org.bukkit.NamespacedKey

class PlayerAcquireItemAdvancementTrigger(key: NamespacedKey) : AdvancementTrigger<PlayerAcquireItemAdvancementTriggerInstance>(key) {
    override fun createTriggerInstance(
        advancement: RebarAdvancement,
        instanceInfo: ConfigSection
    ): PlayerAcquireItemAdvancementTriggerInstance {
        TODO("Not yet implemented")
    }
}