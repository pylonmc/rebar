package io.github.pylonmc.rebar.event.api

import com.destroystokyo.paper.util.SneakyThrow
import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager
import java.lang.invoke.MethodHandles
import kotlin.jvm.java

interface MultiListener : Listener {
    fun register(plugin: Plugin, pluginManager: PluginManager) {
        pluginManager.registerEvents(this, plugin)

        val methods = this::class.java.declaredMethods
        for (method in methods) {
            var priorities: Array<EventPriority>
            var ignoreCancelled = false
            if (method.isAnnotationPresent(MultiHandler::class.java)) {
                val annotation = method.getAnnotation(MultiHandler::class.java)
                priorities = annotation.priorities
                ignoreCancelled = annotation.ignoreCancelled
            } else if (method.isAnnotationPresent(UniversalHandler::class.java)) {
                priorities = EventPriority.entries.toTypedArray()
            } else {
                continue
            }

            if (method.parameterTypes.size != 2 || !Event::class.java.isAssignableFrom(method.parameterTypes[0]) || method.parameterTypes[1] != EventPriority::class.java) {
                throw IllegalStateException("Method ${method.name} in class ${this::class.java.name} must have exactly two parameters, the first being a subclass of Event, and the second being EventPriority")
            } else if (!method.trySetAccessible()) {
                throw IllegalStateException("Could not access method ${method.name} in class ${this::class.java.name}")
            }

            try {
                val lookup = MethodHandles.lookup()
                val methodHandle = lookup.unreflect(method)
                if (method.returnType != Void.TYPE) {
                    plugin.logger.warning("Method ${method.name} in class ${this::class.java.name} is annotated with @MultiHandler/@UniversalHandler but has a non-void return type. The return value will be ignored.")
                }

                @Suppress("UNCHECKED_CAST")
                val eventClass = method.parameterTypes[0] as Class<out Event>
                priorities.forEach { priority ->
                    pluginManager.registerEvent(
                        eventClass,
                        this,
                        priority,
                        { listener, event ->
                            if (eventClass.isInstance(event)) {
                                try {
                                    methodHandle.invoke(listener, event, priority)
                                } catch (t : Throwable) {
                                    SneakyThrow.sneaky(t)
                                }
                            }
                        },
                        plugin,
                        ignoreCancelled
                    )
                }
            } catch (e: IllegalAccessException) {
                throw IllegalStateException("Could not access method ${method.name} in class ${this::class.java.name}", e)
            }
        }
    }
}