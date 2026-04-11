package io.github.pylonmc.rebar.async.schedulers

import com.google.common.collect.Queues
import io.github.pylonmc.rebar.async.RebarScheduledTask
import java.util.*

/**
 * Scheduler using a [PriorityQueue] as a delegate
 *
 * O(log n) insertions and evictions
 */
class PriorityQueueScheduler : Scheduler {
    private val taskQueue = Queues.synchronizedQueue(PriorityQueue<RebarScheduledTask>())

    override fun schedule(executeAt: Long, runnable: Runnable) {
        taskQueue.add(RebarScheduledTask(executeAt, runnable))
    }

    override fun getValid(currentTick: Long): List<RebarScheduledTask> {
        val list = mutableListOf<RebarScheduledTask>()
        while (taskQueue.isNotEmpty() && taskQueue.peek().executeTick <= currentTick) {
            val task = taskQueue.poll()
            list.add(task)
        }

        return list
    }
}