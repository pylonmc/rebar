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

class RebarAdvancement(
    private val key: NamespacedKey,
    val parent: NamespacedKey?,
    val displayInfo: RebarAdvancementDisplayInfo?,
    val rewards: List<RebarAdvancementRewardSpec>,
    val criteria: List<RebarCriteria>,
    /**
     * Which [criteria] must be completed, by name. The outer list is AND, each inner list is OR:
     * `[[a, b], [c]]` means "(a or b) and c". Every criterion must appear in at least one group.
     */
    val requirements: List<List<String>>,
) : Keyed {

    init {
        // Same rules vanilla enforces when loading datapack advancements. The NMS Advancement constructor
        // skips them, and an advancement that breaks them silently becomes impossible to complete
        // (AdvancementRequirements.test returns false when requirements are empty).
        // To make an advancement that only code can unlock, give it a criterion with no triggers.
        require(criteria.isNotEmpty()) {
            "Advancement $key must have at least one criterion (use a criterion with no triggers if it is only unlocked through code)"
        }
        val criterionNames = criteria.map { it.key }
        val duplicates = criterionNames.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        require(duplicates.isEmpty()) { "Advancement $key has duplicate criterion names: $duplicates" }
        require(requirements.isNotEmpty()) { "Advancement $key must have at least one requirements group" }
        require(requirements.none { it.isEmpty() }) { "Advancement $key has an empty requirements group" }
        val referenced = requirements.flatten().toSet()
        val unknown = referenced - criterionNames.toSet()
        require(unknown.isEmpty()) {
            "Advancement $key requirements reference unknown criteria $unknown (criteria: $criterionNames)"
        }
        val unreferenced = criterionNames.toSet() - referenced
        require(unreferenced.isEmpty()) {
            "Advancement $key has criteria $unreferenced that are not in any requirements group"
        }
    }

    override fun getKey(): NamespacedKey {
        return key
    }

    fun register() {
        RebarRegistry.ADVANCEMENTS.register(this)
    }

    fun vanilla(): Advancement = Bukkit.getAdvancement(key) ?: error(
        "Advancement $key is not registered on the server. If its parent was removed by a resource reload, it was " +
                "unregistered; see the warning in the log."
    )

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
