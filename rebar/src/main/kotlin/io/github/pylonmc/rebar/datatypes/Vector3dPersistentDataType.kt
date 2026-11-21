package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.joml.Vector3d

object Vector3dPersistentDataType : PersistentDataType<PersistentDataContainer, Vector3d> {
    val xKey = rebarKey("x", RebarSerializers.DOUBLE)
    val yKey = rebarKey("y", RebarSerializers.DOUBLE)
    val zKey = rebarKey("z", RebarSerializers.DOUBLE)

    override fun getPrimitiveType(): Class<PersistentDataContainer> = PersistentDataContainer::class.java

    override fun getComplexType(): Class<Vector3d> = Vector3d::class.java

    override fun fromPrimitive(primitive: PersistentDataContainer, context: PersistentDataAdapterContext): Vector3d {
        val x = primitive.get(xKey)!!
        val y = primitive.get(yKey)!!
        val z = primitive.get(zKey)!!
        return Vector3d(x, y, z)
    }

    override fun toPrimitive(complex: Vector3d, context: PersistentDataAdapterContext): PersistentDataContainer {
        val pdc = context.newPersistentDataContainer()
        pdc.set(xKey, complex.x)
        pdc.set(yKey, complex.y)
        pdc.set(zKey, complex.z)
        return pdc
    }
}