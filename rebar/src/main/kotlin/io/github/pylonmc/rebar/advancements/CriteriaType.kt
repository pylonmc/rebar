package io.github.pylonmc.rebar.advancements

import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.registry.RebarRegistry
import org.bukkit.Keyed
import org.bukkit.NamespacedKey
import org.jetbrains.annotations.ApiStatus

interface CriteriaType<T : Criterion> : Keyed {
    fun createCriterion(criterionKey: NamespacedKey, config: ConfigSection): T

    @ApiStatus.NonExtendable
    fun register() {
        RebarRegistry.CRITERIA_TYPE.register(this)
    }

    companion object {
        @JvmField val CRITERIA_TYPES_TO_ADVANCEMENTS: HashMap<CriteriaType<*>, ArrayList<RebarAdvancement>> = hashMapOf()
    }
}