package io.github.pylonmc.rebar.async

data class ScheduledTask(val executeTick: Long, val runnable: Runnable) : Comparable<ScheduledTask> {
    override fun compareTo(other: ScheduledTask): Int {
        return executeTick.compareTo(other.executeTick)
    }
}