package io.github.pylonmc.rebar.collections

class LimitedDeque<T> private constructor(
    val maxSize: Int,
    private val deque: ArrayDeque<T> = ArrayDeque()
) : MutableList<T> by deque {
    constructor(maxSize: Int) : this(maxSize, ArrayDeque<T>())

    override fun add(element: T): Boolean {
        if (deque.size == maxSize) {
            deque.removeLast()
        }

        deque.addFirst(element)
        return true
    }

    fun toList(): List<T> = deque.toList()
}