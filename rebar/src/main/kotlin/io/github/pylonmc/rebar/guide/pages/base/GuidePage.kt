package io.github.pylonmc.rebar.guide.pages.base

import io.github.pylonmc.rebar.content.guide.RebarGuide
import net.kyori.adventure.text.Component
import org.bukkit.Keyed
import org.bukkit.entity.Player
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.window.Window

/**
 * Represents a page in the [RebarGuide].
 */
interface GuidePage : Keyed {

    /**
     * The title of this page, displayed at the top of the GUI when the page is open.
     */
    val title: Component
        get() = Component.translatable("${key.namespace}.guide.page.${key.key}")

    /**
     * Should the page be displayed to said player? If not it won't be added to the button list
     */
    fun shouldDisplay(player: Player): Boolean = true

    /**
     * Created the page for the given [player].
     */
    fun getGui(player: Player): Gui

    /**
     * Opens the GUI for a player.
     *
     * WARNING: The UI will break and let people take items out of it if an exception is thrown
     * in this function, so make sure to wrap anything in here in try-catch.
     */
    fun open(player: Player) {
        try {
            Window.builder()
                .setUpperGui(getGui(player))
                .setTitle(title)
                .open(player)
            RebarGuide.addHistory(player, this)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
}