package io.github.pylonmc.rebar.async.dispatchers

import io.github.pylonmc.rebar.util.delayTicks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class CoroutineBackedDispatcher(val context: CoroutineContext) : ScopedFutureDispatcher {

    private val scope = CoroutineScope(context)

    override fun dispatch(delayTicks: Long, runnable: Runnable) {
        scope.launch(context) {
            if (delayTicks > 0) {
                delayTicks(delayTicks)
            }
            runnable.run()
        }
    }
}