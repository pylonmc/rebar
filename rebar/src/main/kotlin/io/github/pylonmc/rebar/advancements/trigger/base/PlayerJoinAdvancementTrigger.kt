package io.github.pylonmc.rebar.advancements.trigger.base

import io.github.pylonmc.rebar.advancements.RebarAdvancement
import io.github.pylonmc.rebar.advancements.trigger.AdvancementTrigger
import io.github.pylonmc.rebar.config.ConfigSection
import org.bukkit.NamespacedKey

class PlayerJoinAdvancementTrigger(key: NamespacedKey) : AdvancementTrigger<PlayerJoinAdvancementTriggerInstance>(key) {
    override fun createTriggerInstance(
        advancement: RebarAdvancement,
        instanceInfo: ConfigSection
    ): PlayerJoinAdvancementTriggerInstance {
        TODO("Not yet implemented")
    }
}