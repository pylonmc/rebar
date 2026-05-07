package io.github.pylonmc.rebar.async

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.plugin.Plugin

class PluginScope(val plugin: Plugin) : ScopedFuture.Scope(null), Listener {

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    override val syncDispatcher = BukkitDispatcherWrapperDispatcher(plugin, false)
    override val asyncDispatcher = BukkitDispatcherWrapperDispatcher(plugin, true)

    @EventHandler
    private fun onPluginDisable(event: PluginDisableEvent) {
        if (event.plugin == plugin) {
            cancel()
        }
    }
}