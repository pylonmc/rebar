package io.github.pylonmc.rebar.advancements

import io.github.pylonmc.rebar.registry.RebarRegistry
import org.bukkit.Bukkit
import org.bukkit.Keyed
import org.bukkit.NamespacedKey
import org.bukkit.advancement.Advancement
import org.bukkit.entity.Player

data class RebarAdvancement(
    private val key: NamespacedKey,
    val parent: NamespacedKey?,
    val displayInfo: RebarAdvancementDisplayInfo?,
    val rewards: RebarAdvancementRewards,
    val criteria: List<Criterion>,
    val requirements: List<List<NamespacedKey>>,
) : Keyed {

    override fun getKey(): NamespacedKey {
        return key
    }

    fun register() {
        RebarRegistry.ADVANCEMENTS.register(this)
    }

    fun vanilla(): Advancement = Bukkit.getAdvancement(key)!!

    fun grant(player: Player) {
        val vanilla = vanilla()
        val progress = player.getAdvancementProgress(vanilla)
        for (criteria in progress.remainingCriteria) {
            progress.awardCriteria(criteria)
        }
    }

    fun isUnlocked(player: Player): Boolean
        = player.getAdvancementProgress(vanilla()).isDone
}
