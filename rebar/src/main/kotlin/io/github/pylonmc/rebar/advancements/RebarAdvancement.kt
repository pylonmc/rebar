package io.github.pylonmc.rebar.advancements

import org.bukkit.Keyed
import org.bukkit.NamespacedKey

data class RebarAdvancement(
    private val key: NamespacedKey,
    val parent: NamespacedKey?,
    val displayInfo: RebarAdvancementDisplayInfo?,
    val rewards: RebarAdvancementRewards,
    val criteria: List<NamespacedKey>,
    val requirements: List<List<String>>,
) : Keyed {

    override fun getKey(): NamespacedKey {
        return key
    }
}
