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
            mutationsSinceCreation++
        }

    @field:Volatile
    var mutationsSinceCreation: Int = 0
        private set

    override fun isEmpty() = size == 0

    @field:Volatile
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
         * Constructs a new BVH tree using the provided elements. In many cases the resulting tree is more efficient than one generated by just [insert]ing
         * into an empty tree. It is thus encouraged to occasionally rebuild trees as they accrue mutations.
         *
         * @param elements the elements to build the tree from
         * @param binCount the number of bins to use in the top-down SAH algorithm. Default 16
         */
        @JvmStatic
        @JvmOverloads
        fun <E : Element> buildFromElements(elements: Collection<E>, binCount: Int = 16): BinaryBvhTree<E> {
            val nodes = elements.map(::Leaf)

            fun sahSplit(nodes: List<TreeNode<E>>): TreeNode<E> {
                if (nodes.size == 1) {
                    return nodes.single()
                }

                data class SahSplit(val axis: Int, val splitIndex: Int, val cost: Float)

                val bins = Array<Array<MutableList<TreeNode<E>>>>(3) { Array(binCount) { mutableListOf() } }
                var bestSplit = SahSplit(-1, -1, Float.POSITIVE_INFINITY)
                for (axis in 0..2) {
                    val min = nodes.minOf { it.boundingBox.centroid[axis] }
                    val max = nodes.maxOf { it.boundingBox.centroid[axis] }
                    val range = max - min
                    if (range <= 0f) continue

                    val axisBins = bins[axis]
                    for (node in nodes) {
                        val bin = ((node.boundingBox.centroid[axis] - min) / range * binCount).toInt().coerceAtMost(binCount - 1)
                        axisBins[bin].add(node)
                    }

                    val binBoundsAndSize = axisBins.map { bin ->
                        bin.map { it.boundingBox }.reduceOrNull(BoundingBox::union) to bin.size
                    }

                    for (splitIndex in 1 until binCount) {
                        val left = binBoundsAndSize.take(splitIndex)
                        val leftElementCount = left.sumOf { it.second }
                        if (leftElementCount == 0) continue

                        val right = binBoundsAndSize.drop(splitIndex)
                        val rightElementCount = right.sumOf { it.second }
                        if (rightElementCount == 0) continue

                        val leftSah = left.mapNotNull { it.first }.reduce(BoundingBox::union).surfaceArea * leftElementCount
                        val rightSah = right.mapNotNull { it.first }.reduce(BoundingBox::union).surfaceArea * rightElementCount
                        val cost = leftSah + rightSah
                        if (cost < bestSplit.cost) {
                            bestSplit = SahSplit(axis, splitIndex, cost)
                        }
                    }
                }

                val (left, right) = if (bestSplit.axis == -1) {
                    // centroids are all the same for some reason, fall back to median split along longest axis
                    val axis = (0..2).maxBy { axis ->
                        nodes.maxOf { it.boundingBox.centroid[axis] } - nodes.minOf { it.boundingBox.centroid[axis] }
                    }
                    val sorted = nodes.sortedBy { it.boundingBox.centroid[axis] }
                    sorted.take(sorted.size / 2) to sorted.drop(sorted.size / 2)
                } else {
                    val bestBins = bins[bestSplit.axis]
                    val left = bestBins.take(bestSplit.splitIndex).flatten()
                    val right = bestBins.drop(bestSplit.splitIndex).flatten()
                    left to right
                }
                return Branch(sahSplit(left), sahSplit(right))
            }

            val tree = BinaryBvhTree<E>()
            tree.size = nodes.size
            tree.tree = when (nodes.size) {
                0 -> null
                1 -> nodes.single()
                else -> sahSplit(nodes)
            }
            tree.mutationsSinceCreation = 0

            return tree
        }
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
        2 * (size.x() * size.y() + size.x() * size.z() + size.y() * size.z())
    }

    val centroid by lazy { (min + max) / 2f }

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