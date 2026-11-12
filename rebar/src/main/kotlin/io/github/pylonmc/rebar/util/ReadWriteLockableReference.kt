package io.github.pylonmc.rebar.util

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
class ReadWriteLockableReference<T>(value: T) {

    val reference = AtomicReference(value)
    val lock = ReentrantReadWriteLock()

    fun readLock(): ReentrantReadWriteLock.ReadLock = lock.readLock()
    fun writeLock(): ReentrantReadWriteLock.WriteLock = lock.writeLock()

    fun get(): T = reference.get()
    fun set(value: T) {
        this.reference.set(value)
    }

    inline fun <R> read(block: (value: T) -> R): R {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        return lock.read { block(get()) }
    }

    inline fun <R> write(block: (ref: AtomicReference<T>) -> R): R {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        return lock.write { block(reference) }
    }
}