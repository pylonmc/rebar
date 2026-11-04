package io.github.pylonmc.rebar.advancements.trigger

import io.github.pylonmc.rebar.advancements.RebarAdvancement
import io.github.pylonmc.rebar.registry.RebarRegistry
import org.bukkit.NamespacedKey

open class AdvancementTriggerInstance(
    val triggerKey: NamespacedKey,
    val advancementKey: NamespacedKey
) {
    fun getAdvancement(): RebarAdvancement = RebarRegistry.ADVANCEMENTS[advancementKey]!!

    /**
     * @brief Marks this trigger as having fired and marks the criteria as complete
     */
    fun trigger(){
        TODO()
    }
}