package io.github.pylonmc.rebar.advancements.reward

import io.github.pylonmc.rebar.advancements.RebarAdvancement
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player

/**
 * One reward of an advancement, created by a [RebarAdvancementReward] from the options in the advancement's config.
 */
abstract class AdvancementRewardInstance(
    /**
     * The key of the [RebarAdvancementReward] that created this instance.
     */
    val rewardKey: NamespacedKey,
    /**
     * The key of the [RebarAdvancement] this reward instance is assigned to.
     */
    val advancementKey: NamespacedKey
) {
    /**
     * Gives this reward to [player]. If this throws, [RebarAdvancement.grantRewards] logs the error and carries on
     * with the advancement's other rewards. The reward is not retried.
     */
    abstract fun grant(player: Player)
}
