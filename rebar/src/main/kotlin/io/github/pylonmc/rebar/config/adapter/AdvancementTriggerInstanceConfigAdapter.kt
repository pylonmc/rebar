package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.advancements.trigger.AdvancementTriggerInstance
import io.github.pylonmc.rebar.registry.RebarRegistry
import org.bukkit.NamespacedKey
import java.lang.reflect.Type

object AdvancementTriggerInstanceConfigAdapter : ConfigAdapter<AdvancementTriggerInstance> {
    override val type: Type
        get() = AdvancementTriggerInstance::class.java

    override fun convert(value: Any): AdvancementTriggerInstance {
        val section = ConfigAdapter.CONFIG_SECTION.convert(value)
        val triggerKey = section.name?.let { NamespacedKey.fromString(it) } ?: throw ExceptionInInitializerError("AdvancementTriggerInstance must have a key")
        val trigger = RebarRegistry.ADVANCEMENT_TRIGGERS[triggerKey] ?: throw ExceptionInInitializerError($"AdvancementTriggerInstance trigger key ${triggerKey} did not correspond to a trigger in RebarRegistry.ADVANCEMENT_TRIGGERS. Did you remember to register your trigger?")
        return trigger.createTriggerInstance(advancement, section)
    }
}