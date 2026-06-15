package io.github.pylonmc.rebar.guide.pages.base

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import xyz.xenondevs.invui.gui.PagedGui
import java.util.UUID
import java.util.WeakHashMap

/**
 * Represents a GuidePage with multiple pages
 * When building your [PagedGui] add a page change handler using [saveCurrentPage] to remember page changes
 * After constructing your [PagedGui], use [loadCurrentPage] to set the correct page for the player
 *
 * For example:
 * ```kotlin
 * override fun getGui(player: Player): Gui {
 *    val gui = PagedGui.items()
 *        .setStructure(...)
 *        ...
 *        .addPageChangeHandler { _, newPage -> saveCurrentPage(player, newPage) }
 *    return gui.build().apply { loadCurrentPage(player, this) }
 * }
 * ```
 */
interface PagedGuidePage : GuidePage {
    fun loadCurrentPage(player: Player, gui: PagedGui<*>) {
        gui.setPage(pageNumbers.getOrPut(this, ::mutableMapOf).getOrDefault(player.uniqueId, 0))
    }

    fun saveCurrentPage(player: Player, page: Int) {
        pageNumbers.getOrPut(this, ::mutableMapOf)[player.uniqueId] = page
    }

    fun resetCurrentPage(player: Player) {
        pageNumbers[this]?.remove(player.uniqueId)
    }

    companion object : Listener {
        val pageNumbers = WeakHashMap<GuidePage, MutableMap<UUID, Int>>()

        @EventHandler
        private fun onPlayerQuit(event: PlayerQuitEvent) {
            val uuid = event.player.uniqueId
            for (map in pageNumbers.values) {
                map.remove(uuid)
            }
        }
    }
}