package io.github.pylonmc.rebar.util

import org.bukkit.util.Vector

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
