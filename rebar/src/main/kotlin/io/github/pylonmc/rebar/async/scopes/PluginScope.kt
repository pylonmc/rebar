package io.github.pylonmc.rebar.async.scopes

import io.github.pylonmc.rebar.async.ScopedFuture
import io.github.pylonmc.rebar.async.dispatchers.BukkitDispatcher
import io.github.pylonmc.rebar.async.dispatchers.CoroutineBackedDispatcher
import kotlinx.coroutines.Dispatchers
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.plugin.Plugin

class PluginScope(val plugin: Plugin) : ScopedFuture.Scope(null), Listener {

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    override val syncDispatcher = BukkitDispatcher(plugin, false)
    override val asyncDispatcher = CoroutineBackedDispatcher(Dispatchers.Default)

    @EventHandler
    private fun onPluginDisable(event: PluginDisableEvent) {
        if (event.plugin == plugin) {
            cancel()
        }
    }
}