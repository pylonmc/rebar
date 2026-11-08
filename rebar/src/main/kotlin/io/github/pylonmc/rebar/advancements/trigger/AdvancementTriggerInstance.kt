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
    val criterionKey: String
) {
    fun getAdvancement(): RebarAdvancement = RebarRegistry.ADVANCEMENTS[advancementKey]!!

    /**
     * Awards [player] the criterion this trigger is part of.
     *
     * @return true if the criterion was newly awarded, false if [player] already had it
     */
    fun trigger(player: Player): Boolean =
        player.getAdvancementProgress(getAdvancement().vanilla()).awardCriteria(criterionKey)
}
