package io.github.pylonmc.rebar.advancements

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey

data class RebarAdvancementDisplayInfo(val icon: RebarAdvancementIcon, val title: Component, val description: Component, val iconFrame: String, val iconBackground: NamespacedKey?, val showToast: Boolean, val announceToChat: Boolean, val hidden: Boolean) {

}

data class RebarAdvancementIcon(val id: NamespacedKey, val count: Int)