package io.github.pylonmc.rebar.advancements.trigger

import io.github.pylonmc.rebar.advancements.RebarAdvancement
import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.registry.RebarRegistry
import org.bukkit.Keyed
import org.bukkit.NamespacedKey

abstract class AdvancementTrigger<TriggerInstanceType: AdvancementTriggerInstance>(val key: NamespacedKey) : Keyed {
    abstract fun createTriggerInstance(advancement: RebarAdvancement, instanceInfo: ConfigSection): TriggerInstanceType
    override fun getKey(): NamespacedKey = key

    fun register(){
        RebarRegistry.ADVANCEMENT_TRIGGERS.register(this)
    }
}