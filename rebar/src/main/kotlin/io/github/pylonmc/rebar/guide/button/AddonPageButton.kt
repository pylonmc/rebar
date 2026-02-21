package io.github.pylonmc.rebar.guide.button

import io.github.pylonmc.rebar.addon.RebarAddon
import io.github.pylonmc.rebar.guide.pages.base.GuidePage
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import xyz.xenondevs.invui.Click

/**
 * A button that opens a page using the addon for the item
 */
class AddonPageButton(val addon: RebarAddon, val page: GuidePage) : GuideButton() {
    override fun getItemProvider(viewer: Player) = ItemStackBuilder.gui(addon.material, addon.key.key)
        .name(addon.displayName)

    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        page.open(player)
    }

    override fun shouldDisplay(player: Player) = page.shouldDisplay(player)

    override fun priority(): Double = 0.0

}