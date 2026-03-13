package io.github.pylonmc.rebar.collections.tasks

interface Scheduler {

    /**
     * Adds a task to the scheduler
     */
    fun schedule(executeAt: Long, runnable: Runnable)

    /**
     * Gets and evicts valid tasks
     */
    fun getValid(currentTick: Long) : List<ScheduledTask>
}