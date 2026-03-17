package io.github.pylonmc.rebar.util

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
import xyz.xenondevs.invui.ClickEvent
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.state.MutableProperty
import xyz.xenondevs.invui.window.Window
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * Represents a snapshot of a [Window] that can be used to restore the window later. This is useful for example when
 * you require player input from the chat while a window is open, and want to restore the window after the input has been received.
 */
@Suppress("UnstableApiUsage")
data class WindowSnapshot(
    val closeable: Boolean,
    val titleSupplier: Supplier<out Component>,
    val openHandlers: List<Runnable>,
    val fallbackWindow: Supplier<out Window?>,
    val closeHandlers: List<Consumer<in InventoryCloseEvent.Reason>>,
    val outsideClickHandlers: List<Consumer<in ClickEvent>>,
    val windowState: MutableProperty<Int>,
    val windowStateChangeHandlers: List<Consumer<in Int>>,
    val upperGui: Gui,
    val lowerGui: Gui
) {

    fun open(player: Player) {
        Window.builder()
            .setCloseable(closeable)
            .setTitleSupplier(titleSupplier)
            .setOpenHandlers(openHandlers)
            .setFallbackWindow(fallbackWindow)
            .setCloseHandlers(closeHandlers)
            .setOutsideClickHandlers(outsideClickHandlers)
            .setWindowState(windowState)
            .setWindowStateChangeHandlers(windowStateChangeHandlers)
            .setUpperGui(upperGui)
            .setLowerGui(lowerGui)
            .open(player)
    }

    companion object {
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun Window.takeSnapshot(): WindowSnapshot {
            val clazz = this::class.java

            fun Class<*>.getFieldValueInThisOrSuperclasses(fieldName: String): Any {
                try {
                    val field = getDeclaredField(fieldName)
                    field.isAccessible = true
                    return field.get(this@takeSnapshot)
                } catch (e: NoSuchFieldException) {
                    val superclass =
                        superclass ?: throw IllegalStateException("Could not find field `$fieldName` in class hierarchy", e)
                    return superclass.getFieldValueInThisOrSuperclasses(fieldName)
                }
            }

            val closeable = this.isCloseable
            val titleSupplier = clazz.getFieldValueInThisOrSuperclasses("titleSupplier") as Supplier<out Component>
            val openHandlers = this.openHandlers
            val fallbackWindow = clazz.getFieldValueInThisOrSuperclasses("fallbackWindow") as Supplier<out Window?>
            val closeHandlers = this.closeHandlers
            val outsideClickHandlers = this.outsideClickHandlers
            val windowState = clazz.getFieldValueInThisOrSuperclasses("serverWindowState") as MutableProperty<Int>
            val windowStateChangeHandlers = this.windowStateChangeHandlers
            val upperGui = clazz.getFieldValueInThisOrSuperclasses("upperGui") as Gui
            val lowerGui = clazz.getFieldValueInThisOrSuperclasses("lowerGui") as Gui

            return WindowSnapshot(
                closeable,
                titleSupplier,
                openHandlers,
                fallbackWindow,
                closeHandlers,
                outsideClickHandlers,
                windowState,
                windowStateChangeHandlers,
                upperGui,
                lowerGui
            )
        }
    }
}
