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
        val toTest = ArrayDeque<TreeNode<E>>()

        tree?.let { toTest.add(it) }
        while (toTest.isNotEmpty()) {
            when (val node = toTest.removeLast()) {
                is Branch -> if (node.boundingBox.rayIntersection(origin, direction) in 0f..1f) {
                    toTest.add(node.left)
                    toTest.add(node.right)
                }

                is Leaf -> node.getIntersection(origin, direction)?.let { intersections.add(node.element to it) }
            }
        }

        intersections.sortBy { it.second.distanceSquared(origin) }
        return intersections
    }

    /**
     * Returns false if the element is already in the tree
     */
    fun insert(element: E): Boolean {
        val element = Leaf(element)
        val tree = this.tree
        if (tree == null) {
            this.tree = element
            size++
            return true
        }

        val path = ArrayDeque<TreeNode<E>>()
        path.add(tree)
        while (true) {
            when (val node = path.last()) {
                is Leaf -> if (node.element == element.element) return false else break
                is Branch -> {
                    val leftBox = node.left.boundingBox
                    val rightBox = node.right.boundingBox
                    val leftCost = leftBox.union(element.boundingBox).surfaceArea - leftBox.surfaceArea
                    val rightCost = rightBox.union(element.boundingBox).surfaceArea - rightBox.surfaceArea
                    if (leftCost < rightCost) {
                        path.add(node.left)
                    } else {
                        path.add(node.right)
                    }
                }
            }
        }

        var oldNode = path.removeLast()
        var newNode = Branch(oldNode, element)
        while (path.isNotEmpty()) {
            val branch = path.removeLast() as Branch
            newNode = if (branch.left == oldNode) {
                Branch(newNode, branch.right)
            } else {
                Branch(branch.left, newNode)
            }
            oldNode = branch
        }

        this.tree = newNode
        size++

        return true
    }

    /**
     * Returns `true` if the element was removed
     */
    fun remove(element: E): Boolean {
        val tree = this.tree ?: return false
        if (tree is Leaf) {
            if (tree.element == element) {
                this.tree = null
                size--
                return true
            } else {
                return false
            }
        }

        val path = findPath(element)?.let(::ArrayDeque) ?: return false
        val leaf = path.removeLast()
        var oldNode = path.removeLast() as Branch
        var newNode = if (oldNode.left == leaf) {
            oldNode.right
        } else {
            oldNode.left
        }
        while (path.isNotEmpty()) {
            val branch = path.removeLast() as Branch
            newNode = if (branch.left == oldNode) {
                Branch(newNode, branch.right)
            } else {
                Branch(branch.left, newNode)
            }
            oldNode = branch
        }

        this.tree = newNode
        size--

        return true
    }

    override fun contains(element: E) = findPath(element) != null

    override fun containsAll(elements: Collection<E>) = elements.all(::contains)

    private fun findPath(element: E): List<TreeNode<E>>? {
        val tree = this.tree ?: return null
        val tempLeaf = Leaf(element)
        var path: List<TreeNode<E>>? = null
        val paths = ArrayDeque<List<TreeNode<E>>>()
        paths.add(listOf(tree))
        while (paths.isNotEmpty()) {
            val candidatePath = paths.removeLast()
            when (val node = candidatePath.last()) {
                is Leaf -> if (node.element == element) {
                    path = candidatePath
                    break
                }

                is Branch -> {
                    val leftInt = node.left.boundingBox.intersectionArea(tempLeaf.boundingBox)
                    val rightInt = node.right.boundingBox.intersectionArea(tempLeaf.boundingBox)

                    if (!(leftInt == 0f && rightInt == 0f)) {
                        if (leftInt == 0f) {
                            paths.add(candidatePath + node.right)
                        } else if (rightInt == 0f) {
                            paths.add(candidatePath + node.left)
                        } else if (leftInt > rightInt) {
                            // search the one with more intersection first
                            paths.add(candidatePath + node.right)
                            paths.add(candidatePath + node.left)
                        } else {
                            paths.add(candidatePath + node.left)
                            paths.add(candidatePath + node.right)
                        }
                    }
                }
            }
        }

        return path
    }

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
            nodes.removeAt(bestJ)
            nodes[bestI] = Branch(i, j)
        }

        tree = nodes.single()
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
                        stack.add(node.left)
                        stack.add(node.right)
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

    fun intersectionArea(other: BoundingBox): Float {
        fun axisIntersection(axis: Int): Float = max(0f, min(this.max[axis], other.max[axis]) - max(this.min[axis], other.min[axis]))
        return axisIntersection(0) * axisIntersection(1) * axisIntersection(2)
    }

    companion object {
        val UNIT = BoundingBox(Vector3f(-0.5f, -0.5f, -0.5f), Vector3f(0.5f, 0.5f, 0.5f))
    }
}