package io.github.pylonmc.rebar.util

import org.bukkit.util.Vector
import org.joml.Vector3f
import org.joml.Vector3fc

// @formatter:off

operator fun Vector.plus(other: Vector) = clone().add(other)
operator fun Vector.minus(other: Vector) = clone().subtract(other)
operator fun Vector.times(scalar: Double) = clone().multiply(scalar)
operator fun Vector.times(scalar: Int) = this * scalar.toDouble()
operator fun Vector.times(scalar: Long) = this * scalar.toDouble()
operator fun Vector.div(scalar: Double) = clone().multiply(1 / scalar)
operator fun Vector.div(scalar: Int) = this / scalar.toDouble()
operator fun Vector.div(scalar: Long) = this / scalar.toDouble()

operator fun Vector.unaryMinus() = clone().multiply(-1.0)
operator fun Vector.unaryPlus() = this

operator fun Double.times(vector: Vector) = vector * this
operator fun Int.times(vector: Vector) = vector * this
operator fun Long.times(vector: Vector) = vector * this
operator fun Double.div(vector: Vector) = this * Vector(1 / vector.x, 1 / vector.y, 1 / vector.z)
operator fun Int.div(vector: Vector) = this.toDouble() / vector
operator fun Long.div(vector: Vector) = this.toDouble() / vector

operator fun Vector3fc.plus(other: Vector3fc): Vector3f = this.add(other, Vector3f())
operator fun Vector3f.plusAssign(other: Vector3fc) { this.add(other) }

operator fun Vector3fc.minus(other: Vector3fc): Vector3f = this.sub(other, Vector3f())
operator fun Vector3f.minusAssign(other: Vector3fc) { this.sub(other) }

operator fun Vector3fc.times(scalar: Float): Vector3f = this.mul(scalar, Vector3f())
operator fun Vector3f.timesAssign(scalar: Float) { this.mul(scalar) }

operator fun Vector3fc.div(scalar: Float): Vector3f = this.div(scalar, Vector3f())
operator fun Vector3f.divAssign(scalar: Float) { this.div(scalar) }
