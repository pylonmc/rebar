package io.github.pylonmc.rebar.advancements.trigger

import io.github.pylonmc.rebar.advancements.RebarAdvancement
import io.github.pylonmc.rebar.registry.RebarRegistry
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player

open class AdvancementTriggerInstance(
    val triggerKey: NamespacedKey,
    val advancementKey: NamespacedKey,
    /**
     * The name of the criterion (within the advancement [advancementKey]) that this trigger awards.
     */
    val criteriaKey: String
) {
    /**
     * @throws IllegalStateException if the advancement isn't registered: it was dropped as an orphan at startup, or
     * unregistered because a resource reload removed its parent. Trigger instances should only be given events through
     * registered advancements, so that this never happens.
     */
    fun getAdvancement(): RebarAdvancement = RebarRegistry.ADVANCEMENTS[advancementKey] ?: error(
        "Advancement $advancementKey is not registered, so its trigger $triggerKey cannot fire. It was probably " +
                "unregistered because its parent is missing; see the warning in the log."
    )

    /**
     * Awards [player] the criterion this trigger is part of. A criterion with several triggers is awarded by
     * whichever fires first; later ones return false.
     *
     * @return true if the criterion was newly awarded, false if [player] already had it
     */
    fun trigger(player: Player): Boolean = getAdvancement().grantCriteria(player, criteriaKey)
}
