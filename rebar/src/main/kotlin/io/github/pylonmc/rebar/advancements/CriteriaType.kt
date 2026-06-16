package io.github.pylonmc.rebar.advancements

import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.registry.RebarRegistry
import org.bukkit.Keyed
import org.bukkit.NamespacedKey

interface CriteriaType<T : Criterion> : Keyed {
    fun createCriterion(criterionKey: NamespacedKey, config: ConfigSection): T

    fun register() {
        RebarRegistry.CRITERIA_TYPE.register(this)
    }

    companion object {
        @JvmField val criteriaTypesToAdvancements: HashMap<CriteriaType<*>, ArrayList<RebarAdvancement>> = hashMapOf()
    }
}