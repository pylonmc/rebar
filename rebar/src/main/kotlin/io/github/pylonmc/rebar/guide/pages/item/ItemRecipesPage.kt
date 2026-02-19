package io.github.pylonmc.rebar.guide.pages.item

import io.github.pylonmc.rebar.content.guide.RebarGuide
import io.github.pylonmc.rebar.guide.pages.base.PagedGuidePage
import io.github.pylonmc.rebar.recipe.FluidOrItem
import io.github.pylonmc.rebar.recipe.RebarRecipe
import io.github.pylonmc.rebar.recipe.RebarRecipe.Companion.priority
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.gui.GuiItems
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.Markers
import xyz.xenondevs.invui.gui.PagedGui

/**
 * Displays all the recipes for the given [stack].
 */
open class ItemRecipesPage(val stack: ItemStack) : PagedGuidePage {

    val pages: MutableList<Gui> = mutableListOf()

    init {
        val recipes = mutableListOf<RebarRecipe>()
        for (type in RebarRegistry.RECIPE_TYPES) {
            for (recipe in type.recipes) {
                if (!recipe.isHidden && recipe.isOutput(stack)) {
                    recipes.add(recipe)
                }
            }
        }
        recipes.sortByDescending { it.priority }
        for (recipe in recipes) {
            val display = recipe.display()
            if (display != null) {
                pages.add(display)
            }
        }
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
        .addIngredient('g', RebarGuide.ingredientsButton(FluidOrItem.of(stack)))
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
        val KEY = rebarKey("item_recipes")
    }
}