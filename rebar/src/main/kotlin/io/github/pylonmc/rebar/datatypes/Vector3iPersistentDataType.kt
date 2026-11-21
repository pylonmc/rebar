package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.joml.Vector3i

object Vector3iPersistentDataType : PersistentDataType<PersistentDataContainer, Vector3i> {
    val xKey = rebarKey("x", RebarSerializers.INTEGER)
    val yKey = rebarKey("y", RebarSerializers.INTEGER)
    val zKey = rebarKey("z", RebarSerializers.INTEGER)

    override fun getPrimitiveType(): Class<PersistentDataContainer> = PersistentDataContainer::class.java

    override fun getComplexType(): Class<Vector3i> = Vector3i::class.java

    override fun fromPrimitive(primitive: PersistentDataContainer, context: PersistentDataAdapterContext): Vector3i {
        val x = primitive.get(xKey)!!
        val y = primitive.get(yKey)!!
        val z = primitive.get(zKey)!!
        return Vector3i(x, y, z)
    }

    override fun toPrimitive(complex: Vector3i, context: PersistentDataAdapterContext): PersistentDataContainer {
        val pdc = context.newPersistentDataContainer()
        pdc.set(xKey, complex.x)
        pdc.set(yKey, complex.y)
        pdc.set(zKey, complex.z)
        return pdc
    }
}