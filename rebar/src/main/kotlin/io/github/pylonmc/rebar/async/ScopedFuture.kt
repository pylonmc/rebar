package io.github.pylonmc.rebar.async

import org.jetbrains.annotations.MustBeInvokedByOverriders
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantLock
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Function
import kotlin.concurrent.withLock

class ScopedFuture<R> private constructor(scope: Scope) : Future<R>, CompletionStage<R> {

    val scope: Scope = FutureScope(scope, this)

    /**
     * Run immediately after the previous stage completes, regardless of success or failure. This is used to trigger the next stage in the chain.
     * [result] is guaranteed to be non-null when this is called, and will contain the result of the previous stage, or the exception if it failed.
     */
    @Volatile
    private var thenRun: () -> Unit = {}

    @Volatile
    private var result: Result<R>? = null

    @Volatile
    private var isCancelled = false

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        if (isDone) return false
        isCancelled = true
        result = null
        thenRun = {}
        return true
    }

    override fun isCancelled(): Boolean = isCancelled

    override fun isDone(): Boolean = result != null || isCancelled

    override fun get(): R = get(Long.MAX_VALUE, TimeUnit.NANOSECONDS)

    override fun get(timeout: Long, unit: TimeUnit): R {
        if (isCancelled) throw CancellationException()
        if (result != null) return result!!.getOrElse { throw ExecutionException(it) }

        val latch = CountDownLatch(1)
        val oldThenRun = thenRun
        thenRun = {
            oldThenRun()
            latch.countDown()
        }
        if (!latch.await(timeout, unit)) {
            throw TimeoutException()
        }

        if (isCancelled) throw CancellationException()
        return result!!.getOrElse { throw ExecutionException(it) }
    }

    fun start() {
        var rootScope = this.scope
        while (rootScope.parent is FutureScope) {
            rootScope = rootScope.parent
        }
        @Suppress("UNCHECKED_CAST") // only way to create a root future is with new(), which always is Unit
        val rootFuture = (rootScope as FutureScope).future as ScopedFuture<Unit>
        rootFuture.complete(Unit)
    }

    private inline fun <U> thenApply(
        dispatcher: ScopedFutureDispatcher,
        crossinline fn: (R) -> U
    ): ScopedFuture<U> {
        val nextFuture = ScopedFuture<U>(scope)
        thenRun = {
            dispatcher.dispatch {
                nextFuture.completeWithResult(result!!.mapCatching(fn))
            }
        }
        return nextFuture
    }

    override fun <U> thenApply(fn: Function<in R, out U>) =
        thenApply(scope.syncDispatcher, fn::apply)

    override fun <U> thenApplyAsync(fn: Function<in R, out U>) =
        thenApply(scope.asyncDispatcher, fn::apply)

    override fun <U> thenApplyAsync(fn: Function<in R, out U>, executor: Executor): CompletionStage<U> =
        toCompletableFuture().thenApplyAsync(fn, executor)

    override fun thenAccept(action: Consumer<in R>): ScopedFuture<Void?> =
        thenApply(scope.syncDispatcher) { action.accept(it); null }

    override fun thenAcceptAsync(action: Consumer<in R>): ScopedFuture<Void?> =
        thenApply(scope.asyncDispatcher) { action.accept(it); null }

    override fun thenAcceptAsync(action: Consumer<in R>, executor: Executor): CompletionStage<Void?> =
        toCompletableFuture().thenAcceptAsync(action, executor)

    override fun thenRun(action: Runnable): ScopedFuture<Void?> =
        thenApply(scope.syncDispatcher) { action.run(); null }

    override fun thenRunAsync(action: Runnable): ScopedFuture<Void?> =
        thenApply(scope.asyncDispatcher) { action.run(); null }

    override fun thenRunAsync(action: Runnable, executor: Executor): CompletionStage<Void?> =
        toCompletableFuture().thenRunAsync(action, executor)

    private inline fun <U, V> thenCombine(
        other: CompletionStage<out U>,
        dispatcher: ScopedFutureDispatcher,
        crossinline fn: (R, U) -> V
    ): ScopedFuture<V> {
        val nextFuture = ScopedFuture<V>(scope)
        thenRun = {
            other.whenComplete { u, ex ->
                dispatcher.dispatch {
                    if (ex != null) {
                        nextFuture.completeExceptionally(ex)
                    } else {
                        nextFuture.completeWithResult(result!!.mapCatching { r -> fn(r, u) })
                    }
                }
            }
        }
        return nextFuture
    }

    override fun <U, V> thenCombine(other: CompletionStage<out U>, fn: BiFunction<in R, in U, out V>) =
        thenCombine(other, scope.syncDispatcher, fn::apply)

    override fun <U, V> thenCombineAsync(other: CompletionStage<out U>, fn: BiFunction<in R, in U, out V>) =
        thenCombine(other, scope.asyncDispatcher, fn::apply)

    override fun <U, V> thenCombineAsync(
        other: CompletionStage<out U>,
        fn: BiFunction<in R, in U, out V>,
        executor: Executor
    ): CompletionStage<V> =
        toCompletableFuture().thenCombineAsync(other, fn, executor)

    override fun <U> thenAcceptBoth(
        other: CompletionStage<out U>,
        action: BiConsumer<in R, in U>
    ): ScopedFuture<Void?> =
        thenCombine(other, scope.syncDispatcher) { r, u -> action.accept(r, u); null }

    override fun <U> thenAcceptBothAsync(
        other: CompletionStage<out U>,
        action: BiConsumer<in R, in U>
    ): ScopedFuture<Void?> =
        thenCombine(other, scope.asyncDispatcher) { r, u -> action.accept(r, u); null }

    override fun <U> thenAcceptBothAsync(
        other: CompletionStage<out U>,
        action: BiConsumer<in R, in U>,
        executor: Executor
    ): CompletionStage<Void?> =
        toCompletableFuture().thenAcceptBothAsync(other, action, executor)

    override fun runAfterBoth(other: CompletionStage<*>, action: Runnable): ScopedFuture<Void?> =
        thenCombine(other, scope.syncDispatcher) { _, _ -> action.run(); null }

    override fun runAfterBothAsync(other: CompletionStage<*>, action: Runnable): ScopedFuture<Void?> =
        thenCombine(other, scope.asyncDispatcher) { _, _ -> action.run(); null }

    override fun runAfterBothAsync(
        other: CompletionStage<*>,
        action: Runnable,
        executor: Executor
    ): CompletionStage<Void?> =
        toCompletableFuture().runAfterBothAsync(other, action, executor)

    private inline fun <U> applyToEither(
        other: CompletionStage<out R>,
        dispatcher: ScopedFutureDispatcher,
        crossinline fn: (R) -> U
    ): ScopedFuture<U> {
        val nextFuture = ScopedFuture<U>(scope)
        val resultLock = ReentrantLock()
        other.whenComplete { r, throwable ->
            dispatcher.dispatch {
                resultLock.withLock {
                    if (nextFuture.result == null) {
                        if (throwable != null) {
                            nextFuture.completeExceptionally(throwable)
                        } else {
                            nextFuture.completeWithResult(runCatching { fn(r) })
                        }
                    }
                }
            }
        }
        thenRun = {
            dispatcher.dispatch {
                resultLock.withLock {
                    if (nextFuture.result == null) {
                        nextFuture.completeWithResult(result!!.mapCatching(fn))
                    }
                }
            }
        }
        return nextFuture
    }

    override fun <U> applyToEither(other: CompletionStage<out R>, fn: Function<in R, U>) =
        applyToEither(other, scope.syncDispatcher, fn::apply)

    override fun <U> applyToEitherAsync(other: CompletionStage<out R>, fn: Function<in R, U>) =
        applyToEither(other, scope.asyncDispatcher, fn::apply)

    override fun <U> applyToEitherAsync(
        other: CompletionStage<out R>,
        fn: Function<in R, U>,
        executor: Executor
    ): CompletionStage<U> =
        toCompletableFuture().applyToEitherAsync(other, fn, executor)

    override fun acceptEither(other: CompletionStage<out R>, action: Consumer<in R>): ScopedFuture<Void?> =
        applyToEither(other, scope.syncDispatcher) { r -> action.accept(r); null }

    override fun acceptEitherAsync(other: CompletionStage<out R>, action: Consumer<in R>): CompletionStage<Void?> =
        applyToEither(other, scope.asyncDispatcher) { r -> action.accept(r); null }

    override fun acceptEitherAsync(
        other: CompletionStage<out R>,
        action: Consumer<in R>,
        executor: Executor
    ): CompletionStage<Void?> =
        toCompletableFuture().acceptEitherAsync(other, action, executor)

    private inline fun runAfterEither(
        other: CompletionStage<*>,
        dispatcher: ScopedFutureDispatcher,
        crossinline action: () -> Unit
    ): ScopedFuture<Void?> {
        val nextFuture = ScopedFuture<Void?>(scope)
        val resultLock = ReentrantLock()
        other.whenComplete { _, _ ->
            dispatcher.dispatch {
                resultLock.withLock {
                    if (nextFuture.result == null) {
                        action()
                        nextFuture.complete(null)
                    }
                }
            }
        }
        thenRun = {
            dispatcher.dispatch {
                resultLock.withLock {
                    if (nextFuture.result == null) {
                        action()
                        nextFuture.complete(null)
                    }
                }
            }
        }
        return nextFuture
    }

    override fun runAfterEither(other: CompletionStage<*>, action: Runnable): ScopedFuture<Void?> =
        runAfterEither(other, scope.syncDispatcher, action::run)

    override fun runAfterEitherAsync(other: CompletionStage<*>, action: Runnable): ScopedFuture<Void?> =
        runAfterEither(other, scope.asyncDispatcher, action::run)

    override fun runAfterEitherAsync(
        other: CompletionStage<*>,
        action: Runnable,
        executor: Executor
    ): CompletionStage<Void?> =
        toCompletableFuture().runAfterEitherAsync(other, action, executor)

    private inline fun <U> thenCompose(
        dispatcher: ScopedFutureDispatcher,
        crossinline fn: (R) -> CompletionStage<U>
    ): ScopedFuture<U> {
        val nextFuture = ScopedFuture<U>(scope)
        thenRun = {
            dispatcher.dispatch {
                val value = result!!.getOrNull()
                if (value != null) {
                    val composedFuture = fn(value)
                    composedFuture.whenComplete { u, ex ->
                        dispatcher.dispatch {
                            if (ex != null) {
                                nextFuture.completeExceptionally(ex)
                            } else {
                                nextFuture.complete(u)
                            }
                        }
                    }
                } else {
                    nextFuture.completeExceptionally(result!!.exceptionOrNull()!!)
                }
            }
        }
        return nextFuture
    }

    override fun <U> thenCompose(fn: Function<in R, out CompletionStage<U>>): ScopedFuture<U> =
        thenCompose(scope.syncDispatcher, fn::apply)

    override fun <U> thenComposeAsync(fn: Function<in R, out CompletionStage<U>>): ScopedFuture<U> =
        thenCompose(scope.asyncDispatcher, fn::apply)

    override fun <U> thenComposeAsync(
        fn: Function<in R, out CompletionStage<U>>,
        executor: Executor
    ): CompletionStage<U> =
        toCompletableFuture().thenComposeAsync(fn, executor)

    private inline fun <U> handle(
        dispatcher: ScopedFutureDispatcher,
        crossinline fn: (R?, Throwable?) -> U
    ): ScopedFuture<U> {
        val nextFuture = ScopedFuture<U>(scope)
        thenRun = {
            dispatcher.dispatch {
                val r = result!!.getOrNull()
                val ex = result!!.exceptionOrNull()
                nextFuture.completeWithResult(runCatching { fn(r, ex) })
            }
        }
        return nextFuture
    }

    override fun <U> handle(fn: BiFunction<in R?, Throwable?, out U>): ScopedFuture<U> =
        handle(scope.syncDispatcher, fn::apply)

    override fun <U> handleAsync(fn: BiFunction<in R?, Throwable?, out U>): ScopedFuture<U> =
        handle(scope.asyncDispatcher, fn::apply)

    override fun <U> handleAsync(fn: BiFunction<in R?, Throwable?, out U>, executor: Executor): CompletionStage<U> =
        toCompletableFuture().handleAsync(fn, executor)

    private inline fun whenComplete(
        dispatcher: ScopedFutureDispatcher,
        crossinline action: (R?, Throwable?) -> Unit
    ): ScopedFuture<R> {
        val nextFuture = ScopedFuture<R>(scope)
        thenRun = {
            dispatcher.dispatch {
                val r = result!!.getOrNull()
                val ex = result!!.exceptionOrNull()
                action(r, ex)
                nextFuture.completeWithResult(result!!)
            }
        }
        return nextFuture
    }

    override fun whenComplete(action: BiConsumer<in R?, in Throwable?>): ScopedFuture<R> =
        whenComplete(scope.syncDispatcher, action::accept)

    override fun whenCompleteAsync(action: BiConsumer<in R?, in Throwable?>): ScopedFuture<R> =
        whenComplete(scope.asyncDispatcher, action::accept)

    override fun whenCompleteAsync(action: BiConsumer<in R?, in Throwable?>, executor: Executor): CompletionStage<R> =
        toCompletableFuture().whenCompleteAsync(action, executor)

    override fun exceptionally(fn: Function<Throwable, out R>): CompletionStage<R> {
        val nextFuture = ScopedFuture<R>(scope)
        thenRun = {
            val ex = result!!.exceptionOrNull()
            if (ex != null) {
                nextFuture.completeWithResult(runCatching { fn.apply(ex) })
            } else {
                nextFuture.completeWithResult(result!!)
            }
        }
        return nextFuture
    }

    fun completeWithResult(result: Result<R>) {
        this.result = result
        thenRun()
    }

    fun complete(value: R) {
        completeWithResult(Result.success(value))
    }

    fun completeExceptionally(ex: Throwable) {
        completeWithResult(Result.failure(ex))
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

    abstract class Scope(val parent: Scope?) {

        init {
            parent?.children?.add(this)
        }

        val children: List<Scope>
            field = mutableListOf()

        abstract val syncDispatcher: ScopedFutureDispatcher
        abstract val asyncDispatcher: ScopedFutureDispatcher

        @MustBeInvokedByOverriders
        open fun cancel() {
            for (child in children) {
                child.cancel()
            }
        }
    }

    private class FutureScope(parent: Scope, val future: ScopedFuture<*>) : Scope(parent) {

        override val syncDispatcher = parent.syncDispatcher
        override val asyncDispatcher = parent.asyncDispatcher

        override fun cancel() {
            super.cancel()
            future.cancel(true)
        }
    }

    companion object {

        @JvmStatic
        fun new(scope: Scope): ScopedFuture<Unit> {
            return ScopedFuture(scope)
        }
    }
}