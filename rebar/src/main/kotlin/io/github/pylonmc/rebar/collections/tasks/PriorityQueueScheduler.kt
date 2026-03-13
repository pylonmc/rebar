package io.github.pylonmc.rebar.collections.tasks

import java.util.*

/**
 * Scheduler using a [PriorityQueue] as a delegate
 *
 * O(log n) insertions and evictions
 */
class PriorityQueueScheduler : Scheduler {
    private val taskQueue = PriorityQueue<ScheduledTask>() // this is not really thread safe, should it be changed?

    override fun schedule(tick: Long, delayTicks: Long, runnable: Runnable) {
        taskQueue.add(ScheduledTask(tick + delayTicks, runnable))
    }

    override fun getValid(tick: Long): List<ScheduledTask> {
        val list = mutableListOf<ScheduledTask>()
        while (taskQueue.isNotEmpty() && taskQueue.peek().executeTick <= tick) {
            val task = taskQueue.poll()
            list.add(task)
        }

        return list
    }
}