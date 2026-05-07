package io.github.pylonmc.rebar.async

import io.github.pylonmc.rebar.Rebar
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantLock
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Function
import kotlin.concurrent.withLock

class RebarFuture<R> private constructor(
    /**
     * Run immediately after the previous stage completes, regardless of success or failure. This is used to trigger the next stage in the chain.
     * [result] is guaranteed to be non-null when this is called, and will contain the result of the previous stage, or the exception if it failed.
     */
    @Volatile private var thenRun: () -> Unit
) : Future<R>, CompletionStage<R> {

    @Volatile
    private var result: Result<R>? = null
        set(value) {
            field = value
            thenRun()
        }

    @Volatile
    private var isCancelled = false

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        if (isDone) return false
        isCancelled = true
        thenRun = {}
        return true
    }

    override fun isCancelled(): Boolean = isCancelled

    override fun isDone(): Boolean = result != null || isCancelled

    override fun get(): R {
        TODO("Not yet implemented")
    }

    override fun get(timeout: Long, unit: TimeUnit): R {
        TODO("Not yet implemented")
    }

    private inline fun <U> thenApply(
        dispatcher: BukkitDispatcherWrapperDispatcher,
        crossinline fn: (R) -> U
    ): RebarFuture<U> {
        val nextFuture = RebarFuture<U> {}
        thenRun = {
            dispatcher.dispatch {
                nextFuture.result = result!!.mapCatching(fn)
            }
        }
        return nextFuture
    }

    override fun <U> thenApply(fn: Function<in R, out U>) =
        thenApply(syncDispatcher, fn::apply)

    override fun <U> thenApplyAsync(fn: Function<in R, out U>) =
        thenApply(asyncDispatcher, fn::apply)

    override fun <U> thenApplyAsync(fn: Function<in R, out U>, executor: Executor): CompletionStage<U> =
        toCompletableFuture().thenApplyAsync(fn, executor)

    override fun thenAccept(action: Consumer<in R>): RebarFuture<Void?> =
        thenApply(syncDispatcher) { action.accept(it); null }

    override fun thenAcceptAsync(action: Consumer<in R>): RebarFuture<Void?> =
        thenApply(asyncDispatcher) { action.accept(it); null }

    override fun thenAcceptAsync(action: Consumer<in R>, executor: Executor): CompletionStage<Void?> =
        toCompletableFuture().thenAcceptAsync(action, executor)

    override fun thenRun(action: Runnable): RebarFuture<Void?> =
        thenApply(syncDispatcher) { action.run(); null }

    override fun thenRunAsync(action: Runnable): RebarFuture<Void?> =
        thenApply(asyncDispatcher) { action.run(); null }

    override fun thenRunAsync(action: Runnable, executor: Executor): CompletionStage<Void?> =
        toCompletableFuture().thenRunAsync(action, executor)

    private inline fun <U, V> thenCombine(
        other: CompletionStage<out U>,
        dispatcher: BukkitDispatcherWrapperDispatcher,
        crossinline fn: (R, U) -> V
    ): RebarFuture<V> {
        val nextFuture = RebarFuture<V> {}
        thenRun = {
            other.whenComplete { u, ex ->
                dispatcher.dispatch {
                    nextFuture.result = if (ex != null) {
                        Result.failure(ex)
                    } else {
                        result!!.mapCatching { r -> fn(r, u) }
                    }
                }
            }
        }
        return nextFuture
    }

    override fun <U, V> thenCombine(other: CompletionStage<out U>, fn: BiFunction<in R, in U, out V>) =
        thenCombine(other, syncDispatcher, fn::apply)

    override fun <U, V> thenCombineAsync(other: CompletionStage<out U>, fn: BiFunction<in R, in U, out V>) =
        thenCombine(other, asyncDispatcher, fn::apply)

    override fun <U, V> thenCombineAsync(
        other: CompletionStage<out U>,
        fn: BiFunction<in R, in U, out V>,
        executor: Executor
    ): CompletionStage<V> =
        toCompletableFuture().thenCombineAsync(other, fn, executor)

    override fun <U> thenAcceptBoth(other: CompletionStage<out U>, action: BiConsumer<in R, in U>): RebarFuture<Void?> =
        thenCombine(other, syncDispatcher) { r, u -> action.accept(r, u); null }

    override fun <U> thenAcceptBothAsync(
        other: CompletionStage<out U>,
        action: BiConsumer<in R, in U>
    ): RebarFuture<Void?> =
        thenCombine(other, asyncDispatcher) { r, u -> action.accept(r, u); null }

    override fun <U> thenAcceptBothAsync(
        other: CompletionStage<out U>,
        action: BiConsumer<in R, in U>,
        executor: Executor
    ): CompletionStage<Void?> =
        toCompletableFuture().thenAcceptBothAsync(other, action, executor)

    override fun runAfterBoth(other: CompletionStage<*>, action: Runnable): RebarFuture<Void?> =
        thenCombine(other, syncDispatcher) { _, _ -> action.run(); null }

    override fun runAfterBothAsync(other: CompletionStage<*>, action: Runnable): RebarFuture<Void?> =
        thenCombine(other, asyncDispatcher) { _, _ -> action.run(); null }

    override fun runAfterBothAsync(
        other: CompletionStage<*>,
        action: Runnable,
        executor: Executor
    ): CompletionStage<Void?> =
        toCompletableFuture().runAfterBothAsync(other, action, executor)

    private inline fun <U> applyToEither(
        other: CompletionStage<out R>,
        dispatcher: BukkitDispatcherWrapperDispatcher,
        crossinline fn: (R) -> U
    ): RebarFuture<U> {
        val nextFuture = RebarFuture<U> {}
        val resultLock = ReentrantLock()
        other.whenComplete { r, throwable ->
            dispatcher.dispatch {
                resultLock.withLock {
                    if (nextFuture.result == null) {
                        nextFuture.result = if (throwable != null) {
                            Result.failure(throwable)
                        } else {
                            Result.success(fn(r))
                        }
                    }
                }
            }
        }
        thenRun = {
            dispatcher.dispatch {
                resultLock.withLock {
                    if (nextFuture.result == null) {
                        nextFuture.result = result!!.mapCatching(fn)
                    }
                }
            }
        }
        return nextFuture
    }

    override fun <U> applyToEither(other: CompletionStage<out R>, fn: Function<in R, U>) =
        applyToEither(other, syncDispatcher, fn::apply)

    override fun <U> applyToEitherAsync(other: CompletionStage<out R>, fn: Function<in R, U>) =
        applyToEither(other, asyncDispatcher, fn::apply)

    override fun <U> applyToEitherAsync(
        other: CompletionStage<out R>,
        fn: Function<in R, U>,
        executor: Executor
    ): CompletionStage<U> =
        toCompletableFuture().applyToEitherAsync(other, fn, executor)

    override fun acceptEither(other: CompletionStage<out R>, action: Consumer<in R>): RebarFuture<Void?> =
        applyToEither(other, syncDispatcher) { r -> action.accept(r); null }

    override fun acceptEitherAsync(other: CompletionStage<out R>, action: Consumer<in R>): CompletionStage<Void?> =
        applyToEither(other, asyncDispatcher) { r -> action.accept(r); null }

    override fun acceptEitherAsync(
        other: CompletionStage<out R>,
        action: Consumer<in R>,
        executor: Executor
    ): CompletionStage<Void?> =
        toCompletableFuture().acceptEitherAsync(other, action, executor)

    private inline fun runAfterEither(
        other: CompletionStage<*>,
        dispatcher: BukkitDispatcherWrapperDispatcher,
        crossinline action: () -> Unit
    ): RebarFuture<Void?> {
        val nextFuture = RebarFuture<Void?> {}
        val resultLock = ReentrantLock()
        other.whenComplete { _, _ ->
            dispatcher.dispatch {
                resultLock.withLock {
                    if (nextFuture.result == null) {
                        nextFuture.result = Result.success(null)
                        action()
                    }
                }
            }
        }
        thenRun = {
            dispatcher.dispatch {
                resultLock.withLock {
                    if (nextFuture.result == null) {
                        action()
                        nextFuture.result = Result.success(null)
                    }
                }
            }
        }
        return nextFuture
    }

    override fun runAfterEither(other: CompletionStage<*>, action: Runnable): RebarFuture<Void?> =
        runAfterEither(other, syncDispatcher, action::run)

    override fun runAfterEitherAsync(other: CompletionStage<*>, action: Runnable): RebarFuture<Void?> =
        runAfterEither(other, asyncDispatcher, action::run)

    override fun runAfterEitherAsync(
        other: CompletionStage<*>,
        action: Runnable,
        executor: Executor
    ): CompletionStage<Void?> =
        toCompletableFuture().runAfterEitherAsync(other, action, executor)

    private inline fun <U> thenCompose(
        dispatcher: BukkitDispatcherWrapperDispatcher,
        crossinline fn: (R) -> CompletionStage<U>
    ): RebarFuture<U> {
        val nextFuture = RebarFuture<U> {}
        thenRun = {
            dispatcher.dispatch {
                val value = result!!.getOrNull()
                if (value != null) {
                    val composedFuture = fn(value)
                    composedFuture.whenComplete { u, ex ->
                        dispatcher.dispatch {
                            nextFuture.result = if (ex != null) {
                                Result.failure(ex)
                            } else {
                                Result.success(u)
                            }
                        }
                    }
                } else {
                    nextFuture.result = Result.failure(result!!.exceptionOrNull()!!)
                }
            }
        }
        return nextFuture
    }

    override fun <U> thenCompose(fn: Function<in R, out CompletionStage<U>>): RebarFuture<U> =
        thenCompose(syncDispatcher, fn::apply)

    override fun <U> thenComposeAsync(fn: Function<in R, out CompletionStage<U>>): RebarFuture<U> =
        thenCompose(asyncDispatcher, fn::apply)

    override fun <U> thenComposeAsync(
        fn: Function<in R, out CompletionStage<U>>,
        executor: Executor
    ): CompletionStage<U> =
        toCompletableFuture().thenComposeAsync(fn, executor)

    private inline fun <U> handle(
        dispatcher: BukkitDispatcherWrapperDispatcher,
        crossinline fn: (R?, Throwable?) -> U
    ): RebarFuture<U> {
        val nextFuture = RebarFuture<U> {}
        thenRun = {
            dispatcher.dispatch {
                val r = result!!.getOrNull()
                val ex = result!!.exceptionOrNull()
                nextFuture.result = Result.success(fn(r, ex))
            }
        }
        return nextFuture
    }

    override fun <U> handle(fn: BiFunction<in R?, Throwable?, out U>): RebarFuture<U> =
        handle(syncDispatcher, fn::apply)

    override fun <U> handleAsync(fn: BiFunction<in R?, Throwable?, out U>): RebarFuture<U> =
        handle(asyncDispatcher, fn::apply)

    override fun <U> handleAsync(fn: BiFunction<in R?, Throwable?, out U>, executor: Executor): CompletionStage<U> =
        toCompletableFuture().handleAsync(fn, executor)

    private inline fun whenComplete(
        dispatcher: BukkitDispatcherWrapperDispatcher,
        crossinline action: (R?, Throwable?) -> Unit
    ): RebarFuture<R> {
        val nextFuture = RebarFuture<R> {}
        thenRun = {
            dispatcher.dispatch {
                val r = result!!.getOrNull()
                val ex = result!!.exceptionOrNull()
                action(r, ex)
                nextFuture.result = result
            }
        }
        return nextFuture
    }

    override fun whenComplete(action: BiConsumer<in R?, in Throwable?>): RebarFuture<R> =
        whenComplete(syncDispatcher, action::accept)

    override fun whenCompleteAsync(action: BiConsumer<in R?, in Throwable?>): RebarFuture<R> =
        whenComplete(asyncDispatcher, action::accept)

    override fun whenCompleteAsync(action: BiConsumer<in R?, in Throwable?>, executor: Executor): CompletionStage<R> =
        toCompletableFuture().whenCompleteAsync(action, executor)

    override fun exceptionally(fn: Function<Throwable, out R>): CompletionStage<R> {
        val nextFuture = RebarFuture<R> {}
        thenRun = {
            val ex = result!!.exceptionOrNull()
            if (ex != null) {
                nextFuture.result = Result.success(fn.apply(ex))
            } else {
                nextFuture.result = result!!
            }
        }
        return nextFuture
    }

    fun complete(value: R) {
        result = Result.success(value)
    }

    fun completeWithResult(result: Result<R>) {
        this.result = result
    }

    fun completeExceptionally(ex: Throwable) {
        result = Result.failure(ex)
    }

    override fun toCompletableFuture(): CompletableFuture<R> {
        val future = CompletableFuture<R>()
        thenRun = {
            result!!.fold(
                onSuccess = { future.complete(it) },
                onFailure = { future.completeExceptionally(it) }
            )
        }
        return future
    }

    companion object {
        private val syncDispatcher = BukkitDispatcherWrapperDispatcher(Rebar, false)
        private val asyncDispatcher = BukkitDispatcherWrapperDispatcher(Rebar, true)

        @JvmStatic
        fun new(): RebarFuture<Unit> {
            val future = RebarFuture<Unit> {}
            future.result = Result.success(Unit)
            return future
        }
    }
}