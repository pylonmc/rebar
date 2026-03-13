package io.github.pylonmc.rebar.collections.tasks

import java.util.concurrent.ConcurrentLinkedQueue


/**
 * This class schedules tasks in ticks and executes them efficiently using a circular array (the wheel).
 * Each slot in the wheel represents a specific tick modulo the wheel size.
 * Tasks are placed into slots based on their target execution tick.
 * On each tick, the wheel checks the current slot and runs any tasks whose execute tick has been reached.
 *
 * O(1) task scheduling and retrieval within a single wheel rotation.
 * We are using power of 2 for faster operations than modulo (even though I doubt there would be much improvement)
 *
 * @param exponent wheel size (wheelSize = 2 ^ exponent)
 */
class TimingWheel(exponent: Int) : Scheduler {
    private val wheelSize = 1 shl exponent
    private val mask = wheelSize - 1
    private val wheel = Array(wheelSize) { ArrayDeque<ScheduledTask>() }
    // use thread safe queue
    private val incoming = ConcurrentLinkedQueue<ScheduledTask>()

    override fun schedule(executeAt: Long, runnable: Runnable) {
        incoming.add(ScheduledTask(executeAt, runnable))
    }

    override fun getValid(currentTick: Long) : List<ScheduledTask> {
        while (true) {
            val task = incoming.poll() ?: break
            val slot = task.executeTick.toInt() and mask
            wheel[slot].add(task)
        }

        val slot = currentTick.toInt() and mask
        val bucket = wheel[slot]
        if (bucket.isEmpty()) {
            return emptyList()
        }

        val iter = bucket.iterator()
        val list = mutableListOf<ScheduledTask>()
        while (iter.hasNext()) {
            val task = iter.next()

            if (task.executeTick <= currentTick) {
                list.add(task)
                iter.remove()
            }
        }

        return list
    }
}