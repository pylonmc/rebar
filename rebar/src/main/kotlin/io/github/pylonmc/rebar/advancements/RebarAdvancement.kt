package io.github.pylonmc.rebar.advancements

import io.github.pylonmc.rebar.advancements.reward.RebarAdvancementReward
import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemSchema
import io.github.pylonmc.rebar.registry.RebarRegistry
import org.bukkit.Bukkit
import org.bukkit.Keyed
import org.bukkit.NamespacedKey
import org.bukkit.advancement.Advancement
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.jetbrains.annotations.Contract
import kotlin.collections.contains

data class RebarAdvancement(
    private val key: NamespacedKey,
    val parent: NamespacedKey?,
    val displayInfo: RebarAdvancementDisplayInfo?,
    val rewards: List<RebarAdvancementRewardSpec>,
    val criteria: List<RebarCriteria>,
    val requirements: List<List<String>>,
) : Keyed {
    override fun getKey(): NamespacedKey {
        return key
    }

    fun register() {
        RebarRegistry.ADVANCEMENTS.register(this)
    }

    fun vanilla(): Advancement = Bukkit.getAdvancement(key)!!

    fun grant(player: Player) {
        val advancementProgress = player.getAdvancementProgress(vanilla())
        advancementProgress.remainingCriteria.forEach {advancementProgress.awardCriteria(it)}
    }

    fun grantRewards(player: Player){
        rewards.forEach { it.grantReward(player)}
    }

    fun isUnlocked(player: Player): Boolean
        = player.getAdvancementProgress(vanilla()).isDone

    fun isCriteriaComplete(player: Player, criteria: String): Boolean = player.getAdvancementProgress(vanilla()).awardedCriteria.contains(criteria)
    fun isCriteriaComplete(player: Player, criteria: RebarCriteria): Boolean = isCriteriaComplete(player, criteria.key)

    companion object {
        @JvmStatic
        @Contract("null -> null")
        fun fromVanilla(advancement: Advancement?): RebarAdvancement? {
            if (advancement == null) return null
            return RebarRegistry.ADVANCEMENTS[advancement.key]
        }

        @JvmStatic
        @Contract("null -> null")
        fun fromKey(key: NamespacedKey?): RebarAdvancement? {
            if(key == null) return null
            return RebarRegistry.ADVANCEMENTS[key]
        }
    }
}
