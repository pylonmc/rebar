package io.github.pylonmc.rebar.util

import org.joml.*

sealed class BinaryBvhTree<E : BinaryBvhTree.Element> {

    interface Element {
        /**
         * The transformation matrix that would need to be applied on a 1x1x1 bounding box to result in this
         * element's bounding box
         */
        val boundingBox: Matrix4fc

        val position: Vector3fc
    }

    private data class Branch<E : Element>(val left: BinaryBvhTree<E>, val right: BinaryBvhTree<E>) : BinaryBvhTree<E>()
    private data class Leaf<E : Element>(val element: E) : BinaryBvhTree<E>()
}

private data class BoundingBox(val min: Vector3fc, val max: Vector3fc) {
    fun rayIntersection(origin: Vector3fc, direction: Vector3fc): Vector3fc? {
        val length = direction.length()
        val norm = direction / length
        val result = Vector2f()
        if (!Intersectionf.intersectRayAab(origin, norm, min, max, result)) return null
        val t = result.x
        if (t !in 0f..<length) return null
        return origin + norm * t
    }

    companion object {
        val UNIT = BoundingBox(Vector3f(-0.5f, -0.5f, -0.5f), Vector3f(0.5f, 0.5f, 0.5f))
    }
}