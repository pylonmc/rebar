package io.github.pylonmc.rebar.advancements

import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.registry.RebarRegistry
import org.bukkit.Keyed
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player

interface Criteria<T : Criterion> : Keyed {
    fun createCriterion(criterionKey: NamespacedKey, config: ConfigSection): T

    fun register() {
        RebarRegistry.CRITERIA.register(this)
    }
}

interface Criterion : Keyed {
    fun grant(player: Player, advancement: RebarAdvancement) : Boolean {
        val vanillaAdv = advancement.vanilla()
        val progress = player.getAdvancementProgress(vanillaAdv)
        return progress.awardCriteria(key.toString())
    }


}

open class BasicCriterion(private val key: NamespacedKey) : Criterion {

    override fun getKey(): NamespacedKey {
        return key
    }
}