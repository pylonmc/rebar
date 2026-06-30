package io.github.pylonmc.rebar.processor

class RebarProcessor {

    internal class RebarProcess(
        var durationTicks: Int,
        var remainingTicks: Int
    )

    @JvmSynthetic
    internal var process: RebarProcess? = null

    private var onStart: Runnable? = null
    private var onCancel: Runnable? = null
    private var onFinish: Runnable? = null

    val isRunning
        get() = process != null

    val durationTicks
        get() = process?.durationTicks

    val durationSeconds
        get() = durationTicks?.let { it / 20.0 }

    val elapsedTicks
        get() = process?.let { it.durationTicks - it.remainingTicks }

    val elapsedSeconds
        get() = elapsedTicks?.let { it / 20.0 }

    /**
     * The proportion from 0-1 of the process that has been completed so far
     */
    val elapsedProportion
        get() = remainingProportion?.let { 1.0 - it }

    val remainingTicks
        get() = process?.remainingTicks

    val remainingSeconds
        get() = remainingTicks?.let { it / 20.0 }

    /**
     * The proportion from 0-1 of the process that is left to go
     */
    val remainingProportion
        get() = process?.let { it.remainingTicks.toDouble() / it.durationTicks }

    fun onStart(runnable: Runnable) { onStart = runnable }
    fun onCancel(runnable: Runnable) { onCancel = runnable }
    fun onFinish(runnable: Runnable) { onFinish = runnable }

    fun start(durationTicks: Int) {
        process = RebarProcess(durationTicks, durationTicks)
        onStart?.run()
    }

    fun cancel() {
        process = null
        onCancel?.run()
    }

    fun tick(ticks: Int) {
        if (process == null) {
            return
        }

        process!!.remainingTicks -= ticks

        if (process!!.remainingTicks <= 0) {
            process = null
            onFinish?.run()
        }
    }
}