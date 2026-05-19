package io.github.pylonmc.rebar.advancements

import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey

data class RebarAdvancement(val parent: NamespacedKey?, val displayInfo: RebarAdvancementDisplayInfo?, val rewards: RebarAdvancementRewards, val criteria: List<NamespacedKey>, val requirements: List<List<String>>) {

}
