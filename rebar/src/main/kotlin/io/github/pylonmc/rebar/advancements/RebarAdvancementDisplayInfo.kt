package io.github.pylonmc.rebar.advancements

import io.github.pylonmc.rebar.item.ItemTypeWrapper
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey

data class RebarAdvancementDisplayInfo(val icon: RebarAdvancementIcon, var title: Component, val description: Component, val iconFrame: String, val iconBackground: NamespacedKey?, val showToast: Boolean, val announceToChat: Boolean, val hidden: Boolean) {

}

data class RebarAdvancementIcon(val itemType: ItemTypeWrapper, val count: Int)