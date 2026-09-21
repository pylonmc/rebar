package io.github.pylonmc.rebar.util

import org.joml.*
import kotlin.math.max
import kotlin.math.min

/**
 * A [BVH tree](https://en.wikipedia.org/wiki/Bounding_volume_hierarchy) storing exactly two children per node.
 * This gives an intersection testing complexity of O(log n).
 */
class BinaryBvhTree<E : BinaryBvhTree.Element> {

    private var tree: TreeNode<E>? = null

    /**
     * Returns the intersection points of the ray and elements in the tree. The list is sorted by distance from
     * the origin.
     *
     * @param origin the origin of the ray
     * @param direction the direction and distance of the ray
     */
    fun getIntersections(origin: Vector3fc, direction: Vector3fc): List<Vector3fc> {
        val intersections = mutableListOf<Vector3fc>()
        val toTest = ArrayDeque<TreeNode<E>>()

        tree?.let { toTest.add(it) }
        while (toTest.isNotEmpty()) {
            when (val node = toTest.removeLast()) {
                is Branch -> if (node.boundingBox.rayIntersection(origin, direction) in 0f..1f) {
                    toTest.add(node.left)
                    toTest.add(node.right)
                }

                is Leaf -> node.getIntersection(origin, direction)?.let { intersections.add(it) }
            }
        }

        intersections.sortBy { it.distanceSquared(origin) }
        return intersections
    }

    fun insert(element: E) {
        val element = Leaf(element)
        val tree = this.tree
        if (tree == null) {
            this.tree = element
            return
        }

        val path = ArrayDeque<TreeNode<E>>()
        path.add(tree)
        while (true) {
            when (val node = path.last()) {
                is Leaf -> break
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
    }

    fun remove(element: E): Boolean {
        val tree = this.tree ?: return false

        val tempLeaf = Leaf(element)
        var path: ArrayDeque<TreeNode<E>>? = null
        val paths = ArrayDeque<List<TreeNode<E>>>()
        paths.add(listOf(tree))
        while (paths.isNotEmpty()) {
            val candidatePath = paths.removeLast()
            when (val node = candidatePath.last()) {
                is Leaf -> if (node.element == element) {
                    path = ArrayDeque(candidatePath)
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

        if (path == null) return false

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

        return true
    }

    interface Element {
        /**
         * The transformation matrix that would need to be applied on a 1x1x1 bounding box to result in this
         * element's bounding box
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