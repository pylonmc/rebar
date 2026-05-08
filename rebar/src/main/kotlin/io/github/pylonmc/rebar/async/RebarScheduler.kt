@file:JvmName("RebarScheduler")

package io.github.pylonmc.rebar.async

import io.github.pylonmc.rebar.async.scopes.PluginScope
import org.bukkit.plugin.Plugin
import java.util.concurrent.Callable

fun runSync(scope: ScopedFuture.Scope, block: Runnable): ScopedFuture<Void?> {
    return ScopedFuture.new(scope).thenRun(block)
}

fun runAsync(scope: ScopedFuture.Scope, block: Runnable): ScopedFuture<Void?> {
    return ScopedFuture.new(scope).thenRunAsync(block)
}

fun runSync(plugin: Plugin, block: Runnable) = runSync(PluginScope(plugin), block)
fun runAsync(plugin: Plugin, block: Runnable) = runAsync(PluginScope(plugin), block)

fun runSync(scope: ScopedFuture.Scope, delayTicks: Long, block: Runnable): ScopedFuture<Void?> {
    return ScopedFuture.new(scope).delay(delayTicks).thenRun(block)
}

fun runAsync(scope: ScopedFuture.Scope, delayTicks: Long, block: Runnable): ScopedFuture<Void?> {
    return ScopedFuture.new(scope).delay(delayTicks).thenRunAsync(block)
}

fun runSync(plugin: Plugin, delayTicks: Long, block: Runnable) = runSync(PluginScope(plugin), delayTicks, block)
fun runAsync(plugin: Plugin, delayTicks: Long, block: Runnable) = runAsync(PluginScope(plugin), delayTicks, block)

fun <T> runSync(scope: ScopedFuture.Scope, block: Callable<T>): ScopedFuture<T> {
    return ScopedFuture.new(scope).thenApply { _ -> block.call() }
}

fun <T> runAsync(scope: ScopedFuture.Scope, block: Callable<T>): ScopedFuture<T> {
    return ScopedFuture.new(scope).thenApplyAsync { _ -> block.call() }
}

fun <T> runSync(plugin: Plugin, block: Callable<T>) = runSync(PluginScope(plugin), block)
fun <T> runAsync(plugin: Plugin, block: Callable<T>) = runAsync(PluginScope(plugin), block)

fun <T> runSync(scope: ScopedFuture.Scope, delayTicks: Long, block: Callable<T>): ScopedFuture<T> {
    return ScopedFuture.new(scope).delay(delayTicks).thenApply { _ -> block.call() }
}

fun <T> runAsync(scope: ScopedFuture.Scope, delayTicks: Long, block: Callable<T>): ScopedFuture<T> {
    return ScopedFuture.new(scope).delay(delayTicks).thenApplyAsync { _ -> block.call() }
}

fun <T> runSync(plugin: Plugin, delayTicks: Long, block: Callable<T>) = runSync(PluginScope(plugin), delayTicks, block)
fun <T> runAsync(plugin: Plugin, delayTicks: Long, block: Callable<T>) = runAsync(PluginScope(plugin), delayTicks, block)