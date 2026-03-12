package io.github.pylonmc.rebar.guide.pages.help.sub

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.guide.pages.base.SimpleStaticGuidePage
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.Material
import xyz.xenondevs.invui.item.Item

object ResearchingHelpPage : SimpleStaticGuidePage(rebarKey("info_researching")) {
    init {
        addButton(Item.simple(ItemStackBuilder.guide(Material.BOOK, Rebar, "info.researches")))
    }
}