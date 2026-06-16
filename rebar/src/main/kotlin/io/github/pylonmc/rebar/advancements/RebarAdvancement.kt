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
        // TODO: way to get parent class from Criterion base class
        criteria.forEach {
            val criteriaType =
                RebarRegistry.CRITERIA_TYPE.first { it.javaClass.typeParameters.first().javaClass.equals(criteria.javaClass) }
            if(CriteriaType.CRITERIA_TYPES_TO_ADVANCEMENTS.contains(criteriaType)){
                val currentList = CriteriaType.CRITERIA_TYPES_TO_ADVANCEMENTS[criteriaType]!!
                if(!currentList.contains(this)){
                    CriteriaType.CRITERIA_TYPES_TO_ADVANCEMENTS[criteriaType]!!.add(this)
                }
            } else {
                CriteriaType.CRITERIA_TYPES_TO_ADVANCEMENTS[criteriaType] = arrayListOf(this)
            }
        }
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
