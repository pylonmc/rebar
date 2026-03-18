package io.github.pylonmc.rebar.guide.pages.fluid

import io.github.pylonmc.rebar.content.guide.RebarGuide
import io.github.pylonmc.rebar.guide.pages.base.PagedGuidePage
import io.github.pylonmc.rebar.recipe.FluidOrItem
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.gui.GuiItems
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.Markers
import xyz.xenondevs.invui.gui.PagedGui

/**
 * Displays all the recipes for the given fluid.
 */
open class FluidRecipesPage(fluidKey: NamespacedKey) : PagedGuidePage {

    val fluid = RebarRegistry.FLUIDS[fluidKey]!!
    val pages: MutableList<Gui>
        get() {
            val pages = mutableListOf<Gui>()
            for (type in RebarRegistry.RECIPE_TYPES) {
                for (recipe in type.recipes) {
                    if (!recipe.isHidden && recipe.isOutput(fluid)) {
                        recipe.display()?.let { pages.add(it) }
                    }
                }
            }
            return pages
        }

    override fun getKey() = KEY

    open fun getHeader(player: Player, pages: List<Gui>) = PagedGui.guisBuilder()
        .setStructure(
            "< b # # g # # s >",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
        )
        .addIngredient('#', GuiItems.background())
        .addIngredient('<', GuiItems.pagePrevious())
        .addIngredient('b', RebarGuide.backButton)
        .addIngredient('g', RebarGuide.ingredientsButton(FluidOrItem.of(fluid, 1000.0)))
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
        val KEY = rebarKey("fluid_recipes")
    }
}