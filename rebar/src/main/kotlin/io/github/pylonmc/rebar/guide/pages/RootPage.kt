package io.github.pylonmc.rebar.guide.pages

import io.github.pylonmc.rebar.collections.LimitedDeque
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.content.guide.RebarGuide
import io.github.pylonmc.rebar.guide.button.PageButton
import io.github.pylonmc.rebar.guide.pages.base.GuidePage
import io.github.pylonmc.rebar.guide.pages.base.SimpleStaticGuidePage
import io.github.pylonmc.rebar.util.gui.GuiItems
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.entity.Player
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.Markers
import xyz.xenondevs.invui.gui.PagedGui
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.window.Window

/**
 * The first page that appears when you open the guide.
 */
class RootPage : SimpleStaticGuidePage(
    rebarKey("root"),
    mutableListOf(RebarGuide.infoButton, RebarGuide.researchesButton, RebarGuide.fluidsButton)
) {

    override fun getGui(player: Player): Gui {
        val buttons = buttonSupplier.get()
        val gui = PagedGui.itemsBuilder()
            .setStructure(
                "# e # # # # # s #",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
            )
            .addIngredient('#', GuiItems.background())
            .addIngredient('e', RebarGuide.mainSettingsButton)
            .addIngredient('s', RebarGuide.searchItemsAndFluidsButton)
            .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
            .addPageChangeHandler { _, newPage -> saveCurrentPage(player, newPage) }

        val content = mutableListOf<Item>()
        for (button in buttons) {
            if (button is PageButton) {
                if (button.page.shouldDisplay(player)) {
                    content.add(button)
                }
            } else {
                content.add(button)
            }
        }

        return gui.setContent(content).build().apply { loadCurrentPage(player, this) }
    }

    override fun open(player: Player) {
        try {
            Window.builder()
                .setUpperGui(getGui(player))
                .setTitle(title)
                .open(player)

            RebarGuide.history[player.uniqueId] = LimitedDeque<GuidePage>(RebarConfig.GuideConfig.HISTORY_SIZE).apply {
                add(this@RootPage)
            }

        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
}