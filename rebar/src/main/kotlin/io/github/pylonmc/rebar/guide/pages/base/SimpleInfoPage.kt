package io.github.pylonmc.rebar.guide.pages.base

import io.github.pylonmc.rebar.content.guide.RebarGuide
import io.github.pylonmc.rebar.util.gui.GuiItems
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.entity.Player
import xyz.xenondevs.invui.gui.Markers
import xyz.xenondevs.invui.gui.PagedGui
import xyz.xenondevs.invui.item.Item

/**
 * A simple guide page used to display additional info about an item or fluid.
 *
 * @see SimpleStaticGuidePage
 */
open class SimpleInfoPage : SimpleStaticGuidePage(rebarKey("info"), mutableListOf()) {
    override fun getHeader(player: Player, buttons: List<Item>) = PagedGui.itemsBuilder()
        .setStructure(
            "< b # # # # # s >",
            "+ + + + + + + + +",
            "+ x x x x x x x +",
            "+ + + + + + + + +",
        )
        .addIngredient('#', GuiItems.background())
        .addIngredient('+', GuiItems.backgroundBlack())
        .addIngredient('<', GuiItems.pagePrevious())
        .addIngredient('b', RebarGuide.backButton)
        .addIngredient('s', RebarGuide.searchItemsAndFluidsButton)
        .addIngredient('>', GuiItems.pageNext())
        .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
        .addPageChangeHandler { _, newPage -> saveCurrentPage(player, newPage) }
}