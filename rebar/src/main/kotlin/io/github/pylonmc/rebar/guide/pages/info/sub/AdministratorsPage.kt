package io.github.pylonmc.rebar.guide.pages.info.sub

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.guide.pages.base.SimpleStaticGuidePage
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.util.rebarKey
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.Material
import xyz.xenondevs.invui.item.Item

object AdministratorsPage : SimpleStaticGuidePage(rebarKey("info_administrators")) {
    init {
        addButton(Item.builder().setItemProvider(ItemStackBuilder.guide(Material.BOOK, Rebar, "info.rebar_setup"))
            .addClickHandler { click -> click.player.sendMessage(Component.translatable("rebar.guide.button.info.rebar_setup.message")
                .clickEvent(ClickEvent.openUrl("https://pylonmc.github.io/home/installing-pylon/"))) }
            .build())
        addButton(Item.builder().setItemProvider(ItemStackBuilder.guide(Material.LECTERN, Rebar, "info.rebar_addons"))
            .addClickHandler { click -> click.player.sendMessage(Component.translatable("rebar.guide.button.info.rebar_addons.message")
                .clickEvent(ClickEvent.openUrl("https://pylonmc.github.io/home/list-of-addons/"))) }
            .build())
        addButton(Item.simple(ItemStackBuilder.guide(Material.PAPER, Rebar, "info.cheat_controls")))
    }
}