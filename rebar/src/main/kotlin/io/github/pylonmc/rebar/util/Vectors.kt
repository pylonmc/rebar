package io.github.pylonmc.rebar.util

import org.bukkit.util.Vector
import org.joml.Vector3f
import org.joml.Vector3i

object Vectors {

    @JvmStatic
    @get:JvmName("zero")
    val zero: Vector
        get() = Vector(0.0, 0.0, 0.0)

    @JvmStatic
    @get:JvmName("positiveX")
    val positiveX: Vector
        get() = Vector(1.0, 0.0, 0.0)

    @JvmStatic
    @get:JvmName("negativeX")
    val negativeX: Vector
        get() = Vector(-1.0, 0.0, 0.0)

    @JvmStatic
    @get:JvmName("positiveY")
    val positiveY: Vector
        get() = Vector(0.0, 1.0, 0.0)

    @JvmStatic
    @get:JvmName("negativeY")
    val negativeY: Vector
        get() = Vector(0.0, -1.0, 0.0)

    @JvmStatic
    @get:JvmName("positiveZ")
    val positiveZ: Vector
        get() = Vector(0.0, 0.0, 1.0)

    @JvmStatic
    @get:JvmName("negativeZ")
    val negativeZ: Vector
        get() = Vector(0.0, 0.0, -1.0)
}

object Vector3is {

    @JvmStatic
    @get:JvmName("zero")
    val zero: Vector3i
        get() = Vector3i(0, 0, 0)

    @JvmStatic
    @get:JvmName("positiveX")
    val positiveX: Vector3i
        get() = Vector3i(1, 0, 0)

    @JvmStatic
    @get:JvmName("negativeX")
    val negativeX: Vector3i
        get() = Vector3i(-1, 0, 0)

    @JvmStatic
    @get:JvmName("positiveY")
    val positiveY: Vector3i
        get() = Vector3i(0, 1, 0)

    @JvmStatic
    @get:JvmName("negativeY")
    val negativeY: Vector3i
        get() = Vector3i(0, -1, 0)

    @JvmStatic
    @get:JvmName("positiveZ")
    val positiveZ: Vector3i
        get() = Vector3i(0, 0, 1)

    @JvmStatic
    @get:JvmName("negativeZ")
    val negativeZ: Vector3i
        get() = Vector3i(0, 0, -1)
}

object Vector3fs {

    @JvmStatic
    @get:JvmName("zero")
    val zero: Vector3f
        get() = Vector3f(0.0f, 0.0f, 0.0f)

    @JvmStatic
    @get:JvmName("positiveX")
    val positiveX: Vector3f
        get() = Vector3f(1.0f, 0.0f, 0.0f)

    @JvmStatic
    @get:JvmName("negativeX")
    val negativeX: Vector3f
        get() = Vector3f(-1.0f, 0.0f, 0.0f)

    @JvmStatic
    @get:JvmName("positiveY")
    val positiveY: Vector3f
        get() = Vector3f(0.0f, 1.0f, 0.0f)

    @JvmStatic
    @get:JvmName("negativeY")
    val negativeY: Vector3f
        get() = Vector3f(0.0f, -1.0f, 0.0f)

    @JvmStatic
    @get:JvmName("positiveZ")
    val positiveZ: Vector3f
        get() = Vector3f(0.0f, 0.0f, 1.0f)

    @JvmStatic
    @get:JvmName("negativeZ")
    val negativeZ: Vector3f
        get() = Vector3f(0.0f, 0.0f, -1.0f)
}