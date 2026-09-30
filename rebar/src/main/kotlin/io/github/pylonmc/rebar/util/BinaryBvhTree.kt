package io.github.pylonmc.rebar.util

import org.joml.*
import kotlin.math.max
import kotlin.math.min

/**
 * A [BVH tree](https://en.wikipedia.org/wiki/Bounding_volume_hierarchy) storing exactly two children per node.
 * This gives a sublinear intersection testing complexity, with best-case O(log2 n) and worst-case O(n).
 */
class BinaryBvhTree<E : BinaryBvhTree.Element> : Collection<E> {

    @field:Volatile
    override var size: Int = 0
        private set(value) {
            field = value
            mutationsSinceLastRebuild++
        }

    @field:Volatile
    var mutationsSinceLastRebuild: Int = 0
        private set

    override fun isEmpty() = size == 0

    private var tree: TreeNode<E>? = null

    /**
     * Returns the intersection points of the ray and elements in the tree. The list is sorted by distance from
     * the origin.
     *
     * @param origin the origin of the ray
     * @param direction the direction and distance of the ray
     */
    fun getIntersections(origin: Vector3fc, direction: Vector3fc): List<Pair<E, Vector3fc>> {
        val intersections = mutableListOf<Pair<E, Vector3fc>>()

        fun test(node: TreeNode<E>?) {
            when (node) {
                null -> {}

                is Branch -> if (node.boundingBox.rayIntersection(origin, direction) in 0f..1f) {
                    test(node.left)
                    test(node.right)
                }

                is Leaf -> node.getIntersection(origin, direction)?.let { intersections.add(node.element to it) }
            }
        }

        test(tree)

        intersections.sortBy { it.second.distanceSquared(origin) }
        return intersections
    }

    /**
     * Returns false if the element is already in the tree
     */
    fun insert(element: E): Boolean {
        if (element in this) return false

        val leaf = Leaf(element)

        fun insert(node: TreeNode<E>?): TreeNode<E> = when (node) {
            null -> leaf
            is Leaf -> Branch(node, leaf)
            is Branch -> {
                val leftBox = node.left.boundingBox
                val rightBox = node.right.boundingBox
                val leftCost = leftBox.union(leaf.boundingBox).surfaceArea - leftBox.surfaceArea
                val rightCost = rightBox.union(leaf.boundingBox).surfaceArea - rightBox.surfaceArea
                if (leftCost < rightCost) {
                    Branch(insert(node.left), node.right)
                } else {
                    Branch(node.left, insert(node.right))
                }
            }
        }

        tree = insert(tree)
        size++

        return true
    }

    /**
     * Returns `true` if the element was removed
     */
    fun remove(element: E): Boolean {
        val leaf = Leaf(element)

        var found = false
        fun remove(node: TreeNode<E>?): TreeNode<E>? = when (node) {
            null -> null

            is Leaf -> if (node.element == element) {
                found = true
                null
            } else {
                node
            }

            is Branch -> {
                val intersectionLeft = node.left.boundingBox.intersectionVolume(leaf.boundingBox)
                val intersectionRight = node.right.boundingBox.intersectionVolume(leaf.boundingBox)
                if (!(intersectionLeft == 0f && intersectionRight == 0f)) {
                    if (intersectionLeft == 0f) {
                        val right = remove(node.right)
                        if (right == null) node.left else Branch(node.left, right)
                    } else if (intersectionRight == 0f) {
                        val left = remove(node.left)
                        if (left == null) node.right else Branch(left, node.right)
                    } else {
                        val left = remove(node.left)
                        val right = remove(node.right)
                        if (left != null) {
                            if (right != null) Branch(left, right) else left
                        } else {
                            right
                        }
                    }
                } else {
                    node
                }
            }
        }

        tree = remove(tree)
        if (found) size--

        return found
    }

    override fun contains(element: E): Boolean {
        val leaf = Leaf(element)
        fun contains(node: TreeNode<E>?): Boolean = when (node) {
            null -> false
            is Leaf -> node.element == element
            is Branch -> {
                val intersectionLeft = node.left.boundingBox.intersectionVolume(leaf.boundingBox)
                val intersectionRight = node.right.boundingBox.intersectionVolume(leaf.boundingBox)
                if (!(intersectionLeft == 0f && intersectionRight == 0f)) {
                    if (intersectionLeft == 0f) {
                        contains(node.right)
                    } else if (intersectionRight == 0f) {
                        contains(node.left)
                    } else if (intersectionLeft > intersectionRight) {
                        contains(node.left) || contains(node.right)
                    } else {
                        contains(node.right) || contains(node.left)
                    }
                } else {
                    false
                }
            }
        }
        return contains(tree)
    }

    override fun containsAll(elements: Collection<E>) = elements.all(::contains)

    /**
     * Rebuilds the tree for higher lookup efficiency. This has a time complexity of O(n^3) so should be used carefully.
     */
    fun rebuild() {
        if (tree == null || tree is Leaf) return

        val nodes = leafIterator().asSequence().toMutableList<TreeNode<E>>()
        tree = null

        while (nodes.size > 1) {
            var bestI = 0
            var bestJ = 1
            var bestCost = Float.POSITIVE_INFINITY

            @Suppress("ReplaceManualRangeWithIndicesCalls")
            for (i in 0 until nodes.size) {
                for (j in (i - REBUILD_SEARCH_WINDOW).coerceAtLeast(0) until (i + REBUILD_SEARCH_WINDOW).coerceAtMost(nodes.size)) {
                    if (i == j) continue
                    val cost = nodes[i].boundingBox.union(nodes[j].boundingBox).surfaceArea
                    if (cost >= bestCost) continue
                    bestI = i
                    bestJ = j
                    bestCost = cost
                }
            }

            val i = nodes[bestI]
            val j = nodes[bestJ]
            nodes[bestI] = Branch(i, j)
            nodes.removeAt(bestJ)
        }

        tree = nodes.single()

        mutationsSinceLastRebuild = 0
    }

    val depth: Int
        get() {
            fun depth(node: TreeNode<E>, depth: Int): Int = when (node) {
                is Branch -> max(depth(node.left, depth + 1), depth(node.right, depth + 1))
                is Leaf -> depth + 1
            }

            return if (tree == null) 0 else depth(tree!!, 0)
        }

    private fun leafIterator() = object : Iterator<Leaf<E>> {
        private val stack = ArrayDeque(listOfNotNull(tree))

        override fun hasNext() = stack.isNotEmpty()

        override fun next(): Leaf<E> {
            while (true) {
                if (stack.isEmpty()) throw NoSuchElementException()
                when (val node = stack.removeLast()) {
                    is Leaf -> return node
                    is Branch -> {
                        stack.add(node.right)
                        stack.add(node.left)
                    }
                }
            }
        }
    }

    /**
     * Iteration is in no particular order
     */
    override fun iterator() = object : Iterator<E> {

        private val it = leafIterator()

        override fun hasNext() = it.hasNext()

        override fun next(): E = it.next().element
    }

    companion object {
        /**
         *  A DFS flattened BVH probably already has pretty good spatial locality, so we can search for the minimum node in a smaller window around the value
         */
        const val REBUILD_SEARCH_WINDOW = 64
    }

    /**
     * An element that can be stored in a [BinaryBvhTree]. **The tree will not work if either
     * [boundingBoxTransform] or [position] change while in the tree.**
     */
    interface Element {
        /**
         * The transformation matrix that would need to be applied on a 1x1x1 bounding box to result in this
         * element's bounding box.
         */
        val boundingBoxTransform: Matrix4fc

        val position: Vector3fc
    }

    private sealed interface TreeNode<E : Element> {
        val boundingBox: BoundingBox
    }

    private class Branch<E : Element>(
        val left: TreeNode<E>,
        val right: TreeNode<E>
    ) : TreeNode<E> {
        override val boundingBox by lazy {
            left.boundingBox.union(right.boundingBox)
        }
    }

    private class Leaf<E : Element>(val element: E) : TreeNode<E> {

        override val boundingBox by lazy {
            val transform = element.boundingBoxTransform
            val unit = BoundingBox.UNIT

            val min = unit.min
            val max = unit.max

            val corners = listOf(
                Vector3f(min.x(), min.y(), min.z()),
                Vector3f(max.x(), min.y(), min.z()),
                Vector3f(min.x(), max.y(), min.z()),
                Vector3f(max.x(), max.y(), min.z()),
                Vector3f(min.x(), min.y(), max.z()),
                Vector3f(max.x(), min.y(), max.z()),
                Vector3f(min.x(), max.y(), max.z()),
                Vector3f(max.x(), max.y(), max.z()),
            )

            val transformed = corners.map {
                transform.transformPosition(it, Vector3f())
            }

            BoundingBox(
                Vector3f(
                    transformed.minOf { it.x() },
                    transformed.minOf { it.y() },
                    transformed.minOf { it.z() },
                ) + element.position,
                Vector3f(
                    transformed.maxOf { it.x() },
                    transformed.maxOf { it.y() },
                    transformed.maxOf { it.z() },
                ) + element.position,
            )
        }


        fun getIntersection(origin: Vector3fc, direction: Vector3fc): Vector3fc? {
            val transform = element.boundingBoxTransform
            val inverseTransform = transform.invertAffine(Matrix4f())
            val origin = origin - element.position

            val localOrigin = inverseTransform.transformPosition(origin, Vector3f())
            val localDirection = inverseTransform.transformDirection(direction, Vector3f())

            val t = BoundingBox.UNIT.rayIntersection(localOrigin, localDirection)
            if (t !in 0f..1f) return null
            val localResult = localOrigin + localDirection * t

            return transform.transformPosition(localResult, Vector3f()) + element.position
        }
    }
}

private data class BoundingBox(val min: Vector3fc, val max: Vector3fc) {

    val surfaceArea by lazy {
        val size = max - min
        2 * (size.x * size.y + size.x * size.z + size.y * size.z)
    }

    fun rayIntersection(origin: Vector3fc, direction: Vector3fc): Float {
        if (origin.x() in min.x()..max.x() && origin.y() in min.y()..max.y() && origin.z() in min.z()..max.z()) return 0f
        val result = Vector2f()
        if (!Intersectionf.intersectRayAab(origin, direction, min, max, result)) return -1f
        return if (result.x < 0f) result.y else result.x
    }

    fun union(other: BoundingBox) = BoundingBox(
        Vector3f(
            min(min.x(), other.min.x()),
            min(min.y(), other.min.y()),
            min(min.z(), other.min.z())
        ),
        Vector3f(
            max(max.x(), other.max.x()),
            max(max.y(), other.max.y()),
            max(max.z(), other.max.z())
        )
    )

    fun intersectionVolume(other: BoundingBox): Float {
        fun axisIntersection(axis: Int): Float = max(0f, min(this.max[axis], other.max[axis]) - max(this.min[axis], other.min[axis]))
        return axisIntersection(0) * axisIntersection(1) * axisIntersection(2)
    }

    companion object {
        val UNIT = BoundingBox(Vector3f(-0.5f, -0.5f, -0.5f), Vector3f(0.5f, 0.5f, 0.5f))
    }
}