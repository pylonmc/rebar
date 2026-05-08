package io.github.pylonmc.rebar.async.dispatchers

import kotlinx.coroutines.*
import java.lang.Runnable
import kotlin.coroutines.CoroutineContext

interface ScopedFutureDispatcher {

    fun dispatch(delayTicks: Long, runnable: Runnable)

    fun dispatch(runnable: Runnable) {
        dispatch(0L, runnable)
    }
}

@OptIn(InternalCoroutinesApi::class, ExperimentalCoroutinesApi::class)
@JvmSynthetic
fun ScopedFutureDispatcher.asCoroutineDispatcher(): CoroutineDispatcher = object : CoroutineDispatcher(), Delay {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        this@asCoroutineDispatcher.dispatch(block)
    }

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        this@asCoroutineDispatcher.dispatch(delayTicks = timeMillis / 50) {
            if (continuation.isActive) {
                with(continuation) { resumeUndispatched(Unit) }
            }
        }
    }
}