package io.github.pylonmc.rebar.entity.display.transform

import org.bukkit.util.Vector
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.properties.Delegates

/**
 * Creates a transformation that represents a line between two points.
 *
 * You must specify [from], [to], and [thickness]; other fields are optional.
 */
open class LineBuilder {

    protected var translation: Vector3f = Vector3f()
    protected lateinit var from: Vector3f
    protected lateinit var to: Vector3f
    protected var thickness by Delegates.notNull<Float>() // stupid kotlin not allowing lateinits for primitive types
    protected var extraLength: Float = 0.0F
    protected var roll: Float = 0.0F

    fun translation(translation: Vector3f) = apply { this.translation = translation }
    fun translation(translation: Vector3d) = translation(Vector3f(translation))
    fun translation(x: Float, y: Float, z: Float) = translation(Vector3f(x, y, z))
    fun translation(x: Double, y: Double, z: Double) = translation(Vector3d(x, y, z))
    fun from(from: Vector3f) = apply { this.from = from }
    fun from(from: Vector3d) = from(Vector3f(from))
    fun from(from: Vector) = from(from.toVector3f())
    fun from(x: Float, y: Float, z: Float) = from(Vector3f(x, y, z))
    fun from(x: Double, y: Double, z: Double) = from(Vector3d(x, y, z))
    fun to(to: Vector3f) = apply { this.to = to }
    fun to(to: Vector3d) = to(Vector3f(to))
    fun to(x: Float, y: Float, z: Float) = to(Vector3f(x, y, z))
    fun to(x: Double, y: Double, z: Double) = to(Vector3d(x, y, z))
    fun to(to: Vector) = to(to.toVector3f())
    fun thickness(thickness: Float) = apply { this.thickness = thickness }
    fun thickness(thickness: Double) = thickness(thickness.toFloat())
    fun extraLength(extraLength: Float) = apply { this.extraLength = extraLength }
    fun extraLength(extraLength: Double) = extraLength(extraLength.toFloat())
    fun roll(roll: Float) = apply { this.roll = roll }
    fun roll(roll: Double) = roll(roll.toFloat())

    open fun build(): TransformBuilder {
        val midpoint = TransformUtil.getMidpoint(from, to).add(translation)
        return TransformBuilder()
            .translate(midpoint)
            .lookAlong(from, to)
            .rotate(0.0F, 0.0F, roll)
            .scale(Vector3f(thickness, thickness, from.distance(to) + extraLength))
    }
}
