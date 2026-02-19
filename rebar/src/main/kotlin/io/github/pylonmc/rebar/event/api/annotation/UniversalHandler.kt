package io.github.pylonmc.rebar.event.api.annotation

import io.github.pylonmc.rebar.event.api.MultiListener
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

/**
 * A variant of [EventHandler] that listens to all [EventPriorities][EventPriority]
 * Must be used with [MultiListener] registered using [MultiListener.register]
 *
 * All methods annotated with this should be formatted as `fun methodName(event: Event, priority: EventPriority)`
 */
annotation class UniversalHandler()
