package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.advancements.RebarCriteria
import org.bukkit.NamespacedKey
import java.lang.reflect.Type

object AdvancementCriteriaConfigAdapter : ConfigAdapter<RebarCriteria> {
    private val TRIGGER_ADAPTER = AdvancementTriggerInstanceConfigAdapter
    override val type: Type
        get() = RebarCriteria::class.java

    override fun convert(value: Any): RebarCriteria {
        val section = ConfigAdapter.CONFIG_SECTION.convert(value)
        val criteriaName = section.name ?: throw ExceptionInInitializerError("RebarCriteria must have a name")
        return RebarCriteria(criteriaName, ConfigAdapter.LIST.from(TRIGGER_ADAPTER).convert(section))
    }
}