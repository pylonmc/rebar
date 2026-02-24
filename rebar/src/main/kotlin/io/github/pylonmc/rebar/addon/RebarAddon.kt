package io.github.pylonmc.rebar.addon

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.i18n.RebarTranslator.Companion.translator
import io.github.pylonmc.rebar.registry.RebarRegistry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Keyed
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.annotations.ApiStatus
import java.util.Locale

/**
 * Welcome to the place where it all begins: the Rebar addon!
 */
interface RebarAddon : Keyed {

    /**
     * Must return `this`
     */
    val javaPlugin: JavaPlugin

    /**
     * The set of [Locale]s this addon has translations for.
     */
    val languages: Set<Locale>

    /**
     * The material to represent this addon in menus.
     */
    val material: Material

    /**
     * The name used to represent this addon in the guide and other places.
     */
    val displayName: TranslatableComponent
        get() = Component.translatable("${key.namespace}.addon")

    /**
     * The name used to represent this addon in the item tooltips.
     * By default, a blue italic `<your-addon-key>.addon` translation key.
     */
    val footerName: TranslatableComponent
        get() = displayName
            .decoration(TextDecoration.ITALIC, true)
            .color(NamedTextColor.BLUE)

    /**
     * If you use something besides the default `<your-addon-key>.addon` translation key for the addon name,
     * set this to true to suppress warnings about the "missing" key.
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("suppressAddonNameWarning")
    val suppressAddonNameWarning: Boolean
        get() = false

    override fun getKey(): NamespacedKey = NamespacedKey(javaPlugin, javaPlugin.name.lowercase())

    /**
     * Must be called as the first thing in your plugin's `onEnable`
     */
    @ApiStatus.NonExtendable
    fun registerWithRebar() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Rebar")) {
            throw IllegalStateException("Rebar is not installed or not enabled (if Rebar is installed, has it errored?)")
        }

        RebarRegistry.ADDONS.register(this)
        if (!suppressAddonNameWarning) {
            for (locale in languages) {
                if (!translator.canTranslate("${key.namespace}.addon", locale)) {
                    Rebar.logger.warning("${key.namespace} is missing the 'addon' translation key for ${locale.displayName}")
                }
            }
        }
    }

    @ApiStatus.Internal
    companion object : Listener {
        @EventHandler
        private fun onPluginDisable(event: PluginDisableEvent) {
            val plugin = event.plugin
            if (plugin is RebarAddon && plugin !is Rebar) {
                BlockStorage.cleanup(plugin)
                RebarRegistry.BLOCKS.unregisterAllFromAddon(plugin)
                EntityStorage.cleanup(plugin)
                RebarRegistry.ENTITIES.unregisterAllFromAddon(plugin)
                RebarRegistry.GAMETESTS.unregisterAllFromAddon(plugin)
                RebarRegistry.ITEMS.unregisterAllFromAddon(plugin)
                RebarRegistry.RECIPE_TYPES.unregisterAllFromAddon(plugin)
                RebarRegistry.ADDONS.unregister(plugin)
            }
        }
    }
}