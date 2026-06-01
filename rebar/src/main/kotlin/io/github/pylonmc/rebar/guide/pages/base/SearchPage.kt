package io.github.pylonmc.rebar.guide.pages.base

import io.github.pylonmc.rebar.content.guide.RebarGuide
import io.github.pylonmc.rebar.fluid.RebarFluid
import io.github.pylonmc.rebar.item.RebarItemSchema
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
            .addIngredient('#', GuiItems.background())
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
                .map { it.first }
        }

        val split = search.split(" ")
        val entries = getItemNamePairs(player, search).toMutableList()

        for (piece in split) {
            if (piece.isEmpty() || (piece[0] == '@' || piece[0] == '$') && piece.length == 1) continue
            val predicate = when(piece[0]) {
                '@' -> { entry: Pair<Item, String> ->
                    getDisplayNamespace(entry.first, player).contains(piece.substring(1), true)
                }
                '$' -> { entry: Pair<Item, String> ->
                    entry.first.getItemProvider(player).get().lore()?.any { it.plainText.contains(piece.substring(1), true) } ?: false
                }
                else -> { entry: Pair<Item, String> ->
                    entry.second.contains(piece, true)
                }
            }
            entries.retainAll(predicate)
        }
        return entries.map { it.first }.toList()
    }

    fun getDisplayNamespace(item: Item, player: Player): String {
        val itemStack = item.getItemProvider(player).get()
        val key = RebarItemSchema.fromStack(itemStack)?.key ?: RebarFluid.fromStack(itemStack)?.key ?: itemStack.type.key
        val addon = RebarRegistry.ADDONS[NamespacedKey(key.namespace, key.namespace)]
        return addon?.let { GlobalTranslator.render(addon.footerName, player.locale()).plainText.lowercase() } ?: key.namespace
    }

    companion object {
        private val searchSpecifiersStack = ItemStackBuilder.gui(Material.PAPER, "guide_search_specifiers")
            .name(Component.translatable("rebar.guide.button.search-specifiers.name"))
            .lore(Component.translatable("rebar.guide.button.search-specifiers.lore"))

        private val searches = mutableMapOf<UUID, String>()
    }
}