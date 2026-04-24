package io.github.pylonmc.rebar.guide.pages.research

import io.github.pylonmc.rebar.guide.button.ItemButton
import io.github.pylonmc.rebar.guide.pages.base.SimpleStaticGuidePage
import io.github.pylonmc.rebar.item.research.Research
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.gui.GuiItems
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.entity.Player
import xyz.xenondevs.invui.item.Item

/**
 * Shows the items that a research unlocks.
 */
class ResearchItemsPage(research: Research) : SimpleStaticGuidePage(
    KEY,
    research.unlocks.map {
        ItemButton(RebarRegistry.ITEMS[it]!!.createNewItem())
    }.toMutableList()
) {

    override val title = research.name

    override fun getHeader(player: Player, buttons: List<Item>) = super.getHeader(player, buttons)
        .addIngredient('s', GuiItems.background())

    companion object {
        val KEY = rebarKey("research_items")
    }
}