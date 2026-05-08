package io.github.pylonmc.rebar.async.dispatchers

import org.bukkit.plugin.Plugin
import java.util.*

class BukkitDispatcher(private val plugin: Plugin, val async: Boolean) : Runnable, ScopedFutureDispatcher {

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

    override fun dispatch(delayTicks: Long, runnable: Runnable) {
        val executeAt = tick + delayTicks
        taskQueue.add(Task(executeAt, runnable))
    }

    private data class Task(val executeAt: Long, val runnable: Runnable) : Comparable<Task> {
        override fun compareTo(other: Task): Int {
            return executeAt.compareTo(other.executeAt)
        }
    }
}