package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.joml.Vector3f

object Vector3fPersistentDataType : PersistentDataType<PersistentDataContainer, Vector3f> {
    val xKey = rebarKey("x")
    val yKey = rebarKey("y")
    val zKey = rebarKey("z")

    override fun getPrimitiveType(): Class<PersistentDataContainer> = PersistentDataContainer::class.java

    override fun getComplexType(): Class<Vector3f> = Vector3f::class.java

    override fun fromPrimitive(primitive: PersistentDataContainer, context: PersistentDataAdapterContext): Vector3f {
        val x = primitive.get(xKey, RebarSerializers.FLOAT)!!
        val y = primitive.get(yKey, RebarSerializers.FLOAT)!!
        val z = primitive.get(zKey, RebarSerializers.FLOAT)!!
        return Vector3f(x, y, z)
    }

    override fun toPrimitive(complex: Vector3f, context: PersistentDataAdapterContext): PersistentDataContainer {
        val pdc = context.newPersistentDataContainer()
        pdc.set(xKey, RebarSerializers.FLOAT, complex.x)
        pdc.set(yKey, RebarSerializers.FLOAT, complex.y)
        pdc.set(zKey, RebarSerializers.FLOAT, complex.z)
        return pdc
    }
}