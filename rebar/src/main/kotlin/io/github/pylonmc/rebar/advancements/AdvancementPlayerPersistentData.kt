package io.github.pylonmc.rebar.advancements

import org.bukkit.NamespacedKey

/**
 * A player's backed-up progress for one Rebar advancement. See [AdvancementProgressManager].
 *
 * @property completedCriteria the names of the criteria the player has been granted
 * @property completed whether the advancement's rewards have been granted
 */
data class AdvancementPlayerPersistentData(
    val key: NamespacedKey,
    val completedCriteria: Set<String>,
    val completed: Boolean
)
