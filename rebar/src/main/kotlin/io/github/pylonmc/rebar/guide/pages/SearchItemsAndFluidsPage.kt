package io.github.pylonmc.rebar.guide.pages

import io.github.pylonmc.rebar.content.guide.RebarGuide
import io.github.pylonmc.rebar.guide.button.FluidButton
import io.github.pylonmc.rebar.guide.button.ItemButton
import io.github.pylonmc.rebar.guide.pages.base.SearchPage
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.plainText
import io.github.pylonmc.rebar.util.rebarKey
import net.kyori.adventure.text.Component
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.entity.Player
import xyz.xenondevs.invui.item.Item

/**
 * Allows you to search all items and fluids by hijacking the anvil GUI.
 */
class SearchItemsAndFluidsPage : SearchPage(rebarKey("search")) {

    fun getItemButtons(player: Player): MutableList<Pair<Item, String>> = RebarRegistry.ITEMS.filter {
        it.key !in RebarGuide.hiddenItems || (it.key in RebarGuide.adminOnlyItems && player.hasPermission("rebar.guide.cheat"))
    }.map { item ->
        val name = GlobalTranslator.render(Component.translatable("${item.key.namespace}.item.${item.key.key}.name"), player.locale())
        ItemButton(item.createNewItem()) to name.plainText.lowercase(player.locale())
    }.toMutableList()

    fun getFluidButtons(player: Player): MutableList<Pair<Item, String>> = RebarRegistry.FLUIDS.filter {
        it.key !in RebarGuide.hiddenFluids
    }.map { fluid ->
        val name = GlobalTranslator.render(Component.translatable("${fluid.key.namespace}.fluid.${fluid.key.key}"), player.locale())
        FluidButton(fluid) to name.plainText.lowercase(player.locale())
    }.toMutableList()

    override fun getItemNamePairs(player: Player, search: String): List<Pair<Item, String>> {
        val list = getItemButtons(player)
        list.addAll(getFluidButtons(player))
        return list
    }
}