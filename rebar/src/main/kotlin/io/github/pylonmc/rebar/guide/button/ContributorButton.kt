package io.github.pylonmc.rebar.guide.button

import io.github.pylonmc.rebar.addon.RebarAddon
import io.github.pylonmc.rebar.config.ContributorConfig
import io.github.pylonmc.rebar.i18n.RebarArgument
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.util.fromMiniMessage
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ResolvableProfile
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import xyz.xenondevs.invui.Click
import xyz.xenondevs.invui.item.AbstractItem

class ContributorButton(val addon: RebarAddon, val contributor: ContributorConfig) : AbstractItem() {
    override fun getItemProvider(viewer: Player) = ItemStackBuilder.of(Material.PLAYER_HEAD)
        .name(contributor.displayName)
        .lore(
            if (contributor.description != null) {
                fromMiniMessage(contributor.description)
            } else {
                Component.translatable("rebar.guide.button.contributor.default-description",
                    RebarArgument.of("contributor", contributor.displayName),
                    RebarArgument.of("addon", addon.displayName))
            }
        )
        .set(DataComponentTypes.PROFILE, ResolvableProfile.resolvableProfile().uuid(contributor.minecraftUUID))
        .hideFromTooltip(DataComponentTypes.PROFILE)

    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        if (contributor.link != null) {
            player.sendMessage(Component.translatable("rebar.guide.button.contributor.link_message")
                .clickEvent(ClickEvent.openUrl(contributor.link)))
        }
    }
}