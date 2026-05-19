package io.github.pylonmc.rebar.advancements

import org.bukkit.NamespacedKey

data class RebarAdvancementRewards(val experience: Int, val recipes: List<NamespacedKey>, val loot: List<NamespacedKey>, val function: NamespacedKey?) {

}
