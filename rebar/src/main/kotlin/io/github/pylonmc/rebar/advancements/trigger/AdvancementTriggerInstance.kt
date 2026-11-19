package io.github.pylonmc.rebar.advancements.trigger

import io.github.pylonmc.rebar.advancements.RebarAdvancement
import io.github.pylonmc.rebar.registry.RebarRegistry
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player

open class AdvancementTriggerInstance(
    /**
     * The key of the [AdvancementTrigger] that created this instance.
     */
    val triggerKey: NamespacedKey,
    /**
     * The key of the [RebarAdvancement] this trigger instance is assigned to
     */
    val advancementKey: NamespacedKey,
    /**
     * The name of the criterion (within the advancement [advancementKey]) that this trigger awards.
     */
    val criteriaKey: String
) {
    /**
     * @throws IllegalStateException if the advancement isn't registered, generally only because it failed to load.
     */
    fun getAdvancement(): RebarAdvancement = RebarRegistry.ADVANCEMENTS[advancementKey] ?: error(
        "Advancement $advancementKey is not registered, so its trigger $triggerKey cannot fire. It probably failed " +
                "to load; see the errors in the log."
    )

    /**
     * Awards [player] the criterion this trigger is part of. A criterion with several triggers is awarded by
     * whichever fires first; later ones return false.
     *
     * @return true if the criterion was newly awarded, false if [player] already had it
     */
    fun trigger(player: Player): Boolean = getAdvancement().grantCriteria(player, criteriaKey)
}
