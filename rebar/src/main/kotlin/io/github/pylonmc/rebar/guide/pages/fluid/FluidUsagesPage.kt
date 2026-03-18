package io.github.pylonmc.rebar.guide.pages.fluid

import io.github.pylonmc.rebar.content.guide.RebarGuide
import io.github.pylonmc.rebar.fluid.RebarFluid
import io.github.pylonmc.rebar.guide.pages.base.PagedGuidePage
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.gui.GuiItems
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.entity.Player
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.Markers
import xyz.xenondevs.invui.gui.PagedGui

/**
 * Displays all the recipes that use the given [fluid].
 */
open class FluidUsagesPage(val fluid: RebarFluid) : PagedGuidePage {

    val pages: MutableList<Gui>
        get() {
            val pages = mutableListOf<Gui>()
            for (type in RebarRegistry.RECIPE_TYPES) {
                for (recipe in type.recipes) {
                    if (!recipe.isHidden && recipe.isInput(fluid)) {
                        recipe.display()?.let { pages.add(it) }
                    }
                }
            }
            return pages
        }

    override fun getKey() = KEY

    open fun getHeader(player: Player, pages: List<Gui>) = PagedGui.guisBuilder()
        .setStructure(
            "< b # # # # # s >",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
        )
        .addIngredient('#', GuiItems.background())
        .addIngredient('<', GuiItems.pagePrevious())
        .addIngredient('b', RebarGuide.backButton)
        .addIngredient('s', RebarGuide.searchItemsAndFluidsButton)
        .addIngredient('>', GuiItems.pageNext())
        .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
        .addPageChangeHandler { _, newPage -> saveCurrentPage(player, newPage) }

    override fun getGui(player: Player): Gui {
        val gui = getHeader(player, pages)
        gui.setContent(pages)
        return gui.build().apply { loadCurrentPage(player, this) }
    }

    companion object {
        val KEY = rebarKey("fluid_usages")
    }
}