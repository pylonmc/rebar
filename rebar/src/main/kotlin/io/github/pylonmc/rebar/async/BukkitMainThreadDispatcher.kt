package io.github.pylonmc.rebar.async

import io.github.pylonmc.rebar.async.schedulers.PriorityQueueScheduler
import io.github.pylonmc.rebar.async.schedulers.Scheduler
import io.github.pylonmc.rebar.async.schedulers.TimingWheel
import kotlinx.coroutines.*
import org.bukkit.plugin.Plugin
import java.lang.Runnable
import kotlin.coroutines.CoroutineContext

/**
 * A [CoroutineDispatcher] that runs on the Bukkit main thread. As it is tied to the server's tick loop (internally converting all "milliseconds" to "ticks"),
 * execution speed is determined by the server's tick rate. This means that if the server's TPS is 10, and a coroutine delays for 1000 milliseconds, it will
 * actually delay for at least 2000 milliseconds, as the server is running at half speed.
 *
 * @param plugin The plugin instance to use for scheduling tasks. If the plugin is disabled, the dispatcher will not execute any tasks.
 * @param tickRate The tick rate to use for scheduling tasks. This determines how often the dispatcher checks for tasks to execute. Measured in ticks.
 */
@OptIn(InternalCoroutinesApi::class)
class BukkitMainThreadDispatcher(private val plugin: Plugin, private val tickRate: Long) : CoroutineDispatcher(), Runnable, Delay {

    // while TimingWheel would work for other tickRates (mainly tickRate % 2 != 0),
    // it would delay their executions, also most tasks use tickRate 1
    private val scheduler: Scheduler = if (tickRate == 1L) {
        TimingWheel(11)
    } else {
        PriorityQueueScheduler()
    }

    private var tick = 0L

    init {
        plugin.server.scheduler.runTaskTimer(plugin, this, 0L, tickRate)
    }

    override fun run() {
        if (!plugin.isEnabled) return
        tick += tickRate
        scheduler.getValid(tick).forEach { it.runnable.run() }
    }

    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        return !plugin.server.isPrimaryThread
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (!plugin.isEnabled) return
        scheduler.schedule(tick, block)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        if (!plugin.isEnabled) return
        val ticks = timeMillis / 50L
        scheduler.schedule(tick + ticks) {
            if (continuation.isActive) {
                with(continuation) { resumeUndispatched(Unit) }
            }
        }
    }
}
