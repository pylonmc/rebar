package io.github.pylonmc.rebar.advancements

import org.bukkit.NamespacedKey

data class RebarAdvancementCriterionInfo(val trigger: NamespacedKey, val conditions: Map<String, String>){}