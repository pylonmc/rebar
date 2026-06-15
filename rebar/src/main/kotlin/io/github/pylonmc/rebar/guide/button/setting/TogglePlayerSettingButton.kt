package io.github.pylonmc.rebar.guide.button.setting

import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.content.guide.RebarGuide.Companion.playGuideSound
import io.github.pylonmc.rebar.i18n.RebarArgument
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.Click
import xyz.xenondevs.invui.item.AbstractItem
import xyz.xenondevs.invui.item.ItemProvider

/**
 * A button which toggles a boolean setting for a player.
 *
 * [key] The namespaced key for this setting. Used for translation keys and persistent data.
 *
 * [isEnabled] Gets the current setting value for a player.
 *
 * [toggle] Toggles the current setting value for a player.
 *
 * [decorator] Determines what the base ItemStack of the button looks like for a player their current setting value.
 * - This is useful for adding visual indicators of the current setting value, such as changing the material or custom model data.
 * - The name and lore of the button will be applied on top of this ItemStack.
 * - By default the button is lime concrete when enabled and red concrete when disabled with custom model data of "{key}_enabled" and "{key}_disabled" respectively.
 *
 * [placeholderProvider] Provides additional placeholders for the translation. (See [net.kyori.adventure.text.TranslatableComponent.arguments] and [RebarArgument])
 */
data class TogglePlayerSettingButton(
    val key: NamespacedKey,

    val isEnabled: (Player) -> Boolean,
    val toggle: (Player) -> Unit,

    val decorator: (Player, Boolean) -> ItemStack = { _, toggled -> ItemStack.of(if (toggled) Material.LIME_CONCRETE else Material.RED_CONCRETE) },
    val placeholderProvider: (Player, Boolean) -> MutableList<ComponentLike> = { _, _ -> mutableListOf() }
) : AbstractItem() {
    override fun getItemProvider(player: Player) : ItemProvider {
        val toggled = isEnabled(player)
        val identifier = if (toggled) "enabled" else "disabled"
        val placeholders = placeholderProvider(player, toggled)
        return ItemStackBuilder.of(decorator(player, toggled))
            .name(Component.translatable("${key.namespace}.guide.button.${key.key}.${identifier}.name").arguments(placeholders))
            .lore(Component.translatable("${key.namespace}.guide.button.${key.key}.${identifier}.lore").arguments(placeholders))
            .addCustomModelDataString("${key}_${identifier}")
    }

    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        toggle(player)
        notifyWindows()
        player.playGuideSound(RebarConfig.GuideConfig.CLICK_BUTTON_SOUND)
    }
}
