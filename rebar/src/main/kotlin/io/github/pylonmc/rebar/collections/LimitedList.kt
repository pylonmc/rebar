package io.github.pylonmc.rebar.collections

class LimitedList<T>(private val maxSize: Int) {
    private val deque = ArrayDeque<T>()

    val size: Int
        get() = deque.size

    fun add(item: T) {
        if (deque.size == maxSize) {
            deque.removeLast()
        }

        deque.addFirst(item)
    }

    fun clear() {
        deque.clear()
    }

    fun removeLast(): T {
        return deque.removeLast()
    }

    fun isEmpty(): Boolean {
        return deque.isEmpty()
    }

    fun toList(): List<T> = deque.toList()
}