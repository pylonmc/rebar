package io.github.pylonmc.rebar.async

interface ScopedFutureDispatcher {

    fun dispatch(delayTicks: Long, runnable: Runnable)

    fun dispatch(runnable: Runnable) {
        dispatch(0L, runnable)
    }
}