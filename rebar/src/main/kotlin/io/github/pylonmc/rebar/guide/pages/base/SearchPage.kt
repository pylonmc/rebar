package io.github.pylonmc.rebar.guide.pages.base

import info.debatty.java.stringsimilarity.JaroWinkler
import io.github.pylonmc.rebar.content.guide.RebarGuide
import io.github.pylonmc.rebar.guide.button.FluidButton
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.gui.GuiItems
import io.github.pylonmc.rebar.util.plainText
import net.kyori.adventure.text.Component
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.Markers
import xyz.xenondevs.invui.gui.PagedGui
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.window.AnvilWindow
import java.util.UUID

/**
 * A page that allows a collection of things (specified by [getItemNamePairs] to be searched.
 *
 * @param key A key that uniquely identifies this page. Used to generate translation keys for
 * this page
 */
abstract class SearchPage(key: NamespacedKey) : SimpleStaticGuidePage(key) {

    abstract fun getItemNamePairs(player: Player, search: String): List<Pair<Item, String>>

    override fun open(player: Player) {
        var firstRename = true
        val search = searches.getOrDefault(player.uniqueId, "")
        val lowerGui = PagedGui.itemsBuilder()
            .setStructure(
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "< # # # B # # # >"
            )
            .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
            .addIngredient('#', GuiItems.background())
            .addIngredient('B', RebarGuide.backButton)
            .addIngredient('<', GuiItems.pagePrevious())
            .addIngredient('>', GuiItems.pageNext())
            .setContent(getItems(player, search))
            .addPageChangeHandler { _, newPage -> saveCurrentPage(player, newPage) }
            .build()
        val upperGui = Gui.builder()
            .setStructure("# S #")
            .addIngredient('S', searchSpecifiersStack)
            .addIngredient('#', GuiItems.background(search))
            .build()
        loadCurrentPage(player, lowerGui)

        try {
            AnvilWindow.builder()
                .setViewer(player)
                .setUpperGui(upperGui)
                .setLowerGui(lowerGui)
                .setTitle(title)
                .addRenameHandler { search ->
                    if (firstRename) {
                        // The first rename happens immediately when the anvil is opened, we need to ignore it
                        firstRename = false
                        return@addRenameHandler
                    }

                    try {
                        searches[player.uniqueId] = search.lowercase(player.locale())
                        lowerGui.setContent(getItems(player, search.lowercase(player.locale())))
                    } catch (t: Throwable) { // If uncaught, will crash the server
                        t.printStackTrace()
                    }
                }
                .open(player)
            RebarGuide.history.getOrPut(player.uniqueId) { mutableListOf() }.add(this)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    open fun getItems(player: Player, search: String): List<Item> {
        if (search.isBlank()) {
            return getItemNamePairs(player, search)
                .sortedBy { it.second }
                .map { it.first }
        }

        val specifiers = mutableListOf<SearchSpecifier>()
        val split = search.split(" ")

        // Because name specifiers can contain spaces, we need to accumulate them until we hit a different specifier
        val nameSearch = StringBuilder()
        fun buildNameSearch() {
            if (!nameSearch.isEmpty()) {
                specifiers.add(SearchSpecifier.ItemName(nameSearch.toString()))
            }
        }

        for (piece in split) {
            if (piece.isEmpty()) continue
            val type = piece[0]
            when(type) {
                '@' -> {
                    buildNameSearch()
                    specifiers.add(SearchSpecifier.Namespace(piece.substring(1)))
                }
                '$' -> {
                    buildNameSearch()
                    specifiers.add(SearchSpecifier.Lore(piece.substring(1)))
                }
                else -> {
                    if (nameSearch.isNotEmpty()) nameSearch.append(" ")
                    nameSearch.append(piece)
                }
            }
        }
        buildNameSearch()

        val entries = getItemNamePairs(player, search).toMutableList()
        for (specifier in specifiers) {
            // Map each entry to its weight for this specifier, excluding non-matching entries
            val weighted = entries.mapNotNull { entry ->
                val weight = specifier.weight(player, entry) ?: return@mapNotNull null
                Pair(entry.first, weight)
            }
            // Remove entries that didn't match this specifier (are not in the weighted list)
            entries.removeIf { entry ->
                weighted.none { it.first == entry.first }
            }
            // Sort remaining entries by their weight for this specifier
            entries.sortBy { entry ->
                weighted.first { it.first == entry.first }.second
            }
        }
        return entries.map { it.first }.toList()
    }

    companion object {
        private val searchSpecifiersStack = ItemStackBuilder.gui(Material.PAPER, "guide_search_specifiers")
            .name(Component.translatable("rebar.guide.button.search-specifiers.name"))
            .lore(Component.translatable("rebar.guide.button.search-specifiers.lore"))

        private val searchAlgorithm = JaroWinkler()
        private val searches = mutableMapOf<UUID, String>()

        private fun weight(search: String, text: String): Double? {
            val distance = searchAlgorithm.distance(text, search)
            val weight = when {
                text == search -> 0.0
                text.startsWith(search) -> 1.0 + distance
                text.contains(search) -> 2.0 + distance
                else -> 3.0 + distance
            }
            return weight.takeIf { weight < 3.15 }
        }
    }

    private fun interface SearchSpecifier {
        fun weight(player: Player, entry: Pair<Item, String>): Double?

        data class ItemName(val filter: String) : SearchSpecifier {
            override fun weight(player: Player, entry: Pair<Item, String>): Double? {
                return weight(filter, entry.second)
            }
        }

        data class Namespace(val filter: String) : SearchSpecifier {
            override fun weight(player: Player, entry: Pair<Item, String>): Double? {
                val item = entry.first
                val key: NamespacedKey = if (item is FluidButton) {
                    item.currentFluid.key
                } else {
                    RebarItem.fromStack(item.getItemProvider(player).get())?.key ?: return null
                }
                val addon = RebarRegistry.ADDONS[NamespacedKey(key.namespace, key.namespace)] ?: return null
                val compactName = GlobalTranslator.render(addon.displayName, player.locale()).plainText.replace(" ", "").lowercase(player.locale())
                return if (key.namespace.startsWith(filter) || compactName.startsWith(filter)) 0.0 else null
            }
        }

        data class Lore(val filter: String) : SearchSpecifier {
            override fun weight(player: Player, entry: Pair<Item, String>): Double? {
                val stack = entry.first.getItemProvider(player).get()
                return stack.lore()?.map {
                    weight(filter, it.plainText.lowercase(player.locale()))
                }?.filterNotNull()?.minOrNull()
            }
        }
    }
}