package io.github.pylonmc.rebar.advancements

import org.bukkit.NamespacedKey
import org.bukkit.inventory.Recipe
import org.bukkit.loot.LootTable

data class RebarAdvancementRewards(
    val experience: Int,
    val recipes: List<Recipe>,
    val loot: List<LootTable>
) {

}
