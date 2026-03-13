package io.github.pylonmc.rebar.collections.tasks

import java.util.*
import java.util.concurrent.PriorityBlockingQueue

/**
 * Scheduler using a [PriorityQueue] as a delegate
 *
 * O(log n) insertions and evictions
 */
class PriorityQueueScheduler : Scheduler {
    private val taskQueue = PriorityQueue<ScheduledTask>() // this is not really thread safe, should it be changed?

    override fun schedule(executeAt: Long, runnable: Runnable) {
        taskQueue.add(ScheduledTask(executeAt, runnable))
    }

    override fun getValid(currentTick: Long): List<ScheduledTask> {
        val list = mutableListOf<ScheduledTask>()
        while (taskQueue.isNotEmpty() && taskQueue.peek().executeTick <= currentTick) {
            val task = taskQueue.poll()
            list.add(task)
        }

        return list
    }
}