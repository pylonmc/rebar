package io.github.pylonmc.rebar.async

data class RebarScheduledTask(val executeTick: Long, val runnable: Runnable) : Comparable<RebarScheduledTask> {
    override fun compareTo(other: RebarScheduledTask): Int {
        return executeTick.compareTo(other.executeTick)
    }
}