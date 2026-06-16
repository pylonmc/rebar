package io.github.pylonmc.rebar.advancements

import org.bukkit.Keyed
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player

interface Criterion : Keyed {
    fun grant(player: Player, advancement: RebarAdvancement) : Boolean {
        val vanillaAdv = advancement.vanilla()
        val progress = player.getAdvancementProgress(vanillaAdv)
        return progress.awardCriteria(key.toString())
    }
}

open class EmptyCriterion(private val key: NamespacedKey) : Criterion {

    override fun getKey(): NamespacedKey {
        return key
    }
}