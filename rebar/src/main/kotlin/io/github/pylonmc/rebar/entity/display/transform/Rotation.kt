package io.github.pylonmc.rebar.entity.display.transform

import org.joml.*

open class Rotation(protected val quaternion: Quaternionf) : TransformComponent {

    constructor(rotation: Vector3f) : this(Quaternionf().rotateXYZ(rotation.x, rotation.y, rotation.z))
    constructor(rotation: Vector3d) : this(Vector3f(rotation))

    constructor(x: Float, y: Float, z: Float) : this(Vector3f(x, y, z))
    constructor(x: Double, y: Double, z: Double) : this(Vector3d(x, y, z))

    constructor(axisAngle: AxisAngle4f) : this(Quaternionf().rotationAxis(axisAngle))
    constructor(axis: Vector3f, angle: Float) : this(Quaternionf().rotationAxis(angle, axis))
    constructor(x: Float, y: Float, z: Float, angle: Float) : this(Quaternionf().rotationAxis(angle, x, y, z))

    constructor(axisAngle: AxisAngle4d) : this(axisAngle.x, axisAngle.y, axisAngle.z, axisAngle.angle)
    constructor(axis: Vector3d, angle: Double) : this(axis.x, axis.y, axis.z, angle)
    constructor(x: Double, y: Double, z: Double, angle: Double) : this(x.toFloat(), y.toFloat(), z.toFloat(), angle.toFloat())

    constructor(rotation: Quaterniond) : this(Quaternionf(rotation))

    override fun apply(matrix: Matrix4f) {
        matrix.mul(Matrix4f().rotate(quaternion))
    }
}