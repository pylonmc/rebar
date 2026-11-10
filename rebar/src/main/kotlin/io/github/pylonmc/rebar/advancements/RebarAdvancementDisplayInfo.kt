package io.github.pylonmc.rebar.advancements

import io.papermc.paper.advancement.AdvancementDisplay
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey

/**
 * Information about how to display a [RebarAdvancement]
 */
data class RebarAdvancementDisplayInfo(
    /**
     * The item icon to show as the badge for this advancement
     */
    val icon: RebarAdvancementIcon,
    /**
     * The title of the advancement
     */
    val title: Component,
    /**
     * The description that appears when you hover over its title in the completion message or in the advancements page
     */
    val description: Component,
    /**
     * The frame around the icon, which represents what kind of advancement it is.
     */
    val iconFrame: AdvancementDisplay.Frame,
    /**
     * The texture tiled behind this advancement's tab, as a texture ID without the `textures/` prefix or `.png` suffix,
     * e.g. `minecraft:gui/advancements/backgrounds/adventure`. Only used on root advancements.
     */
    val background: NamespacedKey?,
    /**
     * Whether to show the toast in the corner of the player's screen when they unlock the advancement.
     */
    val showToast: Boolean,
    /**
     * Whether to announce the advancement's completion to everyone in the server chat.
     */
    val announceToChat: Boolean,
    /**
     * Whether the advancement is a hidden advancement, meaning that it doesn't show
     */
    val hidden: Boolean
)