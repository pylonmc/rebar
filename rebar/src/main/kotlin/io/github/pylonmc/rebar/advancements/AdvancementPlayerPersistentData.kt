package io.github.pylonmc.rebar.advancements

import org.bukkit.NamespacedKey

data class AdvancementPlayerPersistentData(
    val key: NamespacedKey,
    val completedCriteria: List<String>,
    val completed: Boolean
)
