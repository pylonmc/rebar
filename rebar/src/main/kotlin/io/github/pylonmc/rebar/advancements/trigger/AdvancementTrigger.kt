package io.github.pylonmc.rebar.advancements.trigger

import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.registry.RebarRegistry
import org.bukkit.Keyed
import org.bukkit.NamespacedKey

abstract class AdvancementTrigger<TriggerInstanceType: AdvancementTriggerInstance>(private val key: NamespacedKey) : Keyed {
    /**
     * Called while the advancement [advancementKey] is being parsed, so the [io.github.pylonmc.rebar.advancements.RebarAdvancement]
     * does not exist yet. [options] is empty if the trigger was written as a bare key.
     */
    abstract fun createTriggerInstance(advancementKey: NamespacedKey, options: ConfigSection): TriggerInstanceType

    override fun getKey(): NamespacedKey = key

    fun register(){
        RebarRegistry.ADVANCEMENT_TRIGGERS.register(this)
    }
}