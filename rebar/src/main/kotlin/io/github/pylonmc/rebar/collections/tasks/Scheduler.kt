package io.github.pylonmc.rebar.collections.tasks

interface Scheduler {

    /**
     * Adds a task to the scheduler
     */
    fun schedule(tick: Long, delayTicks: Long, runnable: Runnable)

    /**
     * Gets and evicts valid tasks
     */
    fun getValid(tick: Long) : List<ScheduledTask>
}