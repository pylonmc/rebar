package io.github.pylonmc.rebar.entity.display.transform

import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.util.Transformation
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.roundToInt


@Suppress("unused")
object TransformUtil {

    private val AXIS = listOf(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)

    @JvmStatic
    fun yawToCardinalDirection(yaw: Double): Double
        = -(yaw / 90.0F).roundToInt() * 90.0
    @JvmStatic
    fun yawToCardinalDirection(yaw: Float): Float
        = yawToCardinalDirection(yaw.toDouble()).toFloat()

    @JvmStatic
    fun yawToCardinalFace(yaw: Double): BlockFace
        = AXIS[(yaw / 90.0F).roundToInt() and 0x3]
    @JvmStatic
    fun yawToCardinalFace(yaw: Float): BlockFace
        = yawToCardinalFace(yaw.toDouble())

    @JvmStatic
    fun yawAndPitchToFace(yaw: Double, pitch: Double): BlockFace {
        if (pitch > 45) {
            return BlockFace.UP
        }
        if (pitch < -45) {
            return BlockFace.DOWN
        }
        return yawToCardinalFace(yaw)
    }

    @JvmStatic
    fun yawAndPitchToFace(yaw: Float, pitch: Float): BlockFace
        = yawAndPitchToFace(yaw.toDouble(), pitch.toDouble())

    @JvmStatic
    fun rotatedRadius(radius: Float, x: Float, y: Float, z: Float): Vector3f
        = Vector3f(0.0F, 0.0F, radius).rotateX(x).rotateY(y).rotateZ(z)
    @JvmStatic
    fun rotatedRadius(radius: Double, x: Double, y: Double, z: Double): Vector3d
        = Vector3d(0.0, 0.0, radius).rotateX(x).rotateY(y).rotateZ(z)
    @JvmStatic
    fun rotatedRadius(radius: Float, y: Float): Vector3f
        = Vector3f(0.0F, 0.0F, radius).rotateY(y)
    @JvmStatic
    fun rotatedRadius(radius: Double, y: Double): Vector3d
        = Vector3d(0.0, 0.0, radius).rotateY(y)

    @JvmStatic
    fun getDisplacement(from: Location, to: Location): Vector3f
        = to.clone().subtract(from).toVector().toVector3f()
    @JvmStatic
    fun getDisplacement(from: Vector3f, to: Vector3f): Vector3f
        = Vector3f(to).sub(from)
    @JvmStatic
    fun getDisplacement(from: Vector3d, to: Vector3d): Vector3d
        = Vector3d(to).sub(from)

    @JvmStatic
    fun getDirection(from: Location, to: Location): Vector3f
        = getDisplacement(from, to).normalize()
    @JvmStatic
    fun getDirection(from: Vector3f, to: Vector3f): Vector3f
        = getDisplacement(from, to).normalize()
    @JvmStatic
    fun getDirection(from: Vector3d, to: Vector3d): Vector3d
        = getDisplacement(from, to).normalize()

    @JvmStatic
    fun getMidpoint(from: Location, to: Location): Location {
        return from.clone().add(to).multiply(0.5)
    }
    @JvmStatic
    fun getMidpoint(from: Vector3f, to: Vector3f): Vector3f {
        return Vector3f(from).add(to).mul(0.5f)
    }
    @JvmStatic
    fun getMidpoint(from: Vector3d, to: Vector3d): Vector3d {
        return Vector3d(from).add(to).mul(0.5)
    }

    @JvmStatic
    @JvmName("transformationToMatrix")
    fun Transformation.toMatrix(): Matrix4f {
        return Matrix4f()
            .translate(translation)
            .rotate(leftRotation)
            .scale(scale)
            .rotate(rightRotation)
    }
}