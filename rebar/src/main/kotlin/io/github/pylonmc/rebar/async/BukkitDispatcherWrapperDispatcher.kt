package io.github.pylonmc.rebar.async

import org.bukkit.plugin.Plugin
import java.util.*

// TODO better name
class BukkitDispatcherWrapperDispatcher(private val plugin: Plugin, val async: Boolean) : Runnable {

    private val taskQueue = PriorityQueue<Task>()

    private var tick = 0L

    init {
        if (async) {
            plugin.server.scheduler.runTaskTimerAsynchronously(plugin, this, 0L, 1L)
        } else {
            plugin.server.scheduler.runTaskTimer(plugin, this, 0L, 1L)
        }
    }

    override fun run() {
        if (!plugin.isEnabled) return
        tick++
        while (taskQueue.isNotEmpty() && taskQueue.peek().executeAt <= tick) {
            val task = taskQueue.poll()
            task.runnable.run()
        }
    }

    @JvmOverloads
    fun dispatch(delayTicks: Long = 0L, runnable: Runnable) {
        val executeAt = tick + delayTicks
        taskQueue.add(Task(executeAt, runnable))
    }

    fun cancel(runnable: Runnable) {
        taskQueue.removeIf { it.runnable === runnable }
    }

    private data class Task(val executeAt: Long, val runnable: Runnable) : Comparable<Task> {
        override fun compareTo(other: Task): Int {
            return executeAt.compareTo(other.executeAt)
        }
    }
}