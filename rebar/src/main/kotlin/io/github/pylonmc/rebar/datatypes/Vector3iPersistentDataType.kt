package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.joml.Vector3i

object Vector3iPersistentDataType : PersistentDataType<PersistentDataContainer, Vector3i> {
    val xKey = rebarKey("x")
    val yKey = rebarKey("y")
    val zKey = rebarKey("z")

    override fun getPrimitiveType(): Class<PersistentDataContainer> = PersistentDataContainer::class.java

    override fun getComplexType(): Class<Vector3i> = Vector3i::class.java

    override fun fromPrimitive(primitive: PersistentDataContainer, context: PersistentDataAdapterContext): Vector3i {
        val x = primitive.get(xKey, RebarSerializers.INTEGER)!!
        val y = primitive.get(yKey, RebarSerializers.INTEGER)!!
        val z = primitive.get(zKey, RebarSerializers.INTEGER)!!
        return Vector3i(x, y, z)
    }

    override fun toPrimitive(complex: Vector3i, context: PersistentDataAdapterContext): PersistentDataContainer {
        val pdc = context.newPersistentDataContainer()
        pdc.set(xKey, RebarSerializers.INTEGER, complex.x)
        pdc.set(yKey, RebarSerializers.INTEGER, complex.y)
        pdc.set(zKey, RebarSerializers.INTEGER, complex.z)
        return pdc
    }
}