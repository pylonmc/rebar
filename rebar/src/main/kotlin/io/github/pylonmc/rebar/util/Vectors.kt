package io.github.pylonmc.rebar.util

import org.bukkit.util.Vector
import org.joml.Vector3i

object Vectors {

    @JvmStatic
    val zero: Vector
        get() = Vector(0.0, 0.0, 0.0)

    @JvmStatic
    val positiveX: Vector
        get() = Vector(1.0, 0.0, 0.0)

    @JvmStatic
    val negativeX: Vector
        get() = Vector(-1.0, 0.0, 0.0)

    @JvmStatic
    val positiveY: Vector
        get() = Vector(0.0, 1.0, 0.0)

    @JvmStatic
    val negativeY: Vector
        get() = Vector(0.0, -1.0, 0.0)

    @JvmStatic
    val positiveZ: Vector
        get() = Vector(0.0, 0.0, 1.0)

    @JvmStatic
    val negativeZ: Vector
        get() = Vector(0.0, 0.0, -1.0)
}

object Vector3is {

    @JvmStatic
    val zero: Vector3i
        get() = Vector3i(0, 0, 0)

    @JvmStatic
    val positiveX: Vector3i
        get() = Vector3i(1, 0, 0)

    @JvmStatic
    val negativeX: Vector3i
        get() = Vector3i(-1, 0, 0)

    @JvmStatic
    val positiveY: Vector3i
        get() = Vector3i(0, 1, 0)

    @JvmStatic
    val negativeY: Vector3i
        get() = Vector3i(0, -1, 0)

    @JvmStatic
    val positiveZ: Vector3i
        get() = Vector3i(0, 0, 1)

    @JvmStatic
    val negativeZ: Vector3i
        get() = Vector3i(0, 0, -1)
}