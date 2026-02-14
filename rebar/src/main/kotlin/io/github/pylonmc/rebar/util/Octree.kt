package io.github.pylonmc.rebar.util

import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import java.util.concurrent.CopyOnWriteArraySet

open class Octree<N>(
    private val maxDepth: Int = DEFAULT_MAX_DEPTH,
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,

    private var bounds: BoundingBox,
    private val depth: Int,
    private val entryStrategy: (N) -> BoundingBox,
    private val storeOutOfBoundsEntries: Boolean = false
) : Iterable<N> {
    private var entries: MutableSet<N> = CopyOnWriteArraySet()
    private var outOfBoundsEntries: MutableSet<N> = CopyOnWriteArraySet()
    private var children: Array<Octree<N>>? = null

    constructor(bounds: BoundingBox, depth: Int, entryStrategy: (N) -> BoundingBox) : this(
        DEFAULT_MAX_DEPTH,
        DEFAULT_MAX_ENTRIES,
        bounds,
        depth,
        entryStrategy
    )

    open fun insert(entry: N) : Boolean {
        val entryBounds = entryStrategy(entry)
        if (!bounds.overlaps(entryBounds)) {
            if (storeOutOfBoundsEntries) outOfBoundsEntries.add(entry)
            return false
        }

        if (children != null) {
            for (child in children!!) {
                if (child.bounds.overlaps(entryBounds)) {
                    return child.insert(entry)
                }
            }
        }

        if (entries.size < maxEntries || depth >= maxDepth) {
            entries.add(entry)
            return true
        }

        subdivide()
        return insert(entry)
    }

    open fun remove(entry: N): Boolean {
        val entryBounds = entryStrategy(entry)
        if (!bounds.overlaps(entryBounds)) {
            if (storeOutOfBoundsEntries) outOfBoundsEntries.remove(entry)
            return false
        }

        if (entries.remove(entry)) {
            return true
        }

        if (children != null) {
            for (child in children!!) {
                if (child.bounds.overlaps(entryBounds)) {
                    if (child.remove(entry)) {
                        return true
                    }
                }
            }
        }

        return false
    }

    open fun resize(newBounds: BoundingBox) {
        if (bounds == newBounds) return

        val allEntries = entries.toList().plus(outOfBoundsEntries)
        clear()

        bounds = newBounds
        for (entry in allEntries) {
            insert(entry)
        }
    }

    open fun clear() {
        entries.clear()
        outOfBoundsEntries.clear()
        children?.forEach { it.clear() }
        children = null
    }

    private fun subdivide() {
        val min = bounds.min
        val max = bounds.max
        val center = min.clone().add(max).multiply(0.5)

        children = Array(8) { i ->
            val dx = (i shr 2) and 1
            val dy = (i shr 1) and 1
            val dz = i and 1
            val childMin = Vector(
                if (dx == 0) min.getX() else center.getX(),
                if (dy == 0) min.getY() else center.getY(),
                if (dz == 0) min.getZ() else center.getZ()
            )
            val childMax = Vector(
                if (dx == 0) center.getX() else max.getX(),
                if (dy == 0) center.getY() else max.getY(),
                if (dz == 0) center.getZ() else max.getZ()
            )
            Octree(BoundingBox.of(childMin, childMax), depth + 1, entryStrategy)
        }

        for (entry in entries) {
            for (child in children!!) {
                if (child.bounds.overlaps(entryStrategy(entry))) {
                    child.insert(entry)
                    break
                }
            }
        }
        entries.clear()
    }

    fun query(range: BoundingBox): MutableSet<N> {
        val result = mutableSetOf<N>()
        if (!bounds.overlaps(range)) return result

        for (entry in entries) {
            if (range.overlaps(entryStrategy(entry))) {
                result.add(entry)
            }
        }

        if (children != null) {
            for (child in children!!) {
                result.addAll(child.query(range))
            }
        }

        return result
    }

    fun maxDepth(): Int {
        return children?.maxOfOrNull { it.maxDepth() } ?: depth
    }

    fun allEntries(): Set<N> {
        val result = mutableSetOf<N>()
        result.addAll(entries)
        if (storeOutOfBoundsEntries) result.addAll(outOfBoundsEntries)
        children?.forEach { result.addAll(it.allEntries()) }
        return result
    }

    override fun iterator(): Iterator<N> = OctreeIterator(this)

    companion object {
        // LISTEN IF THESE ARE INSANE FIX THEM, THIS ISN'T MY STRONG SUIT - JustAHuman
        private const val DEFAULT_MAX_DEPTH = 2048
        private const val DEFAULT_MAX_ENTRIES = 128
    }

    private class OctreeIterator<N>(root: Octree<N>) : Iterator<N> {
        private val stack = ArrayDeque<Octree<N>>()
        private var currentEntries: Iterator<N>? = null

        init {
            stack.add(root)
        }

        override fun hasNext(): Boolean {
            while (true) {
                if (currentEntries?.hasNext() == true) {
                    return true
                }
                if (stack.isEmpty()) {
                    return false
                }
                val node = stack.removeLast()
                currentEntries = node.entries.iterator()
                node.children?.let { stack.addAll(it) }
            }
        }

        override fun next(): N {
            if (!hasNext()) throw NoSuchElementException()
            return currentEntries!!.next()
        }
    }
}