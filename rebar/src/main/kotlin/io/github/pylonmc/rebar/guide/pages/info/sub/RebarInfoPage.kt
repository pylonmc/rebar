package io.github.pylonmc.rebar.guide.pages.info.sub

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.content.guide.RebarGuide
import io.github.pylonmc.rebar.guide.pages.base.SimpleStaticGuidePage
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.Material
import xyz.xenondevs.invui.item.Item

object RebarInfoPage : SimpleStaticGuidePage(rebarKey("info_rebar")) {
    init {
        addButton(Item.simple(ItemStackBuilder.guide(Material.BOOK, Rebar, "info.rebar")))
        addButton(Item.simple(ItemStackBuilder.guide(Material.LECTERN, Rebar, "info.guide_controls")))
        addButton(Item.builder().setItemProvider(ItemStackBuilder.guide(Material.PAPER, Rebar, "info.settings"))
            .addClickHandler { click -> RebarGuide.mainSettingsPage.open(click.player) }
            .build())
    }
}