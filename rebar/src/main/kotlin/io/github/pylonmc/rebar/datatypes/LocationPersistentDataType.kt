package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.Location
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

object LocationPersistentDataType : PersistentDataType<PersistentDataContainer, Location> {
    val xKey = rebarKey("x", RebarSerializers.DOUBLE)
    val yKey = rebarKey("y", RebarSerializers.DOUBLE)
    val zKey = rebarKey("z", RebarSerializers.DOUBLE)
    val yawKey = rebarKey("yaw", RebarSerializers.FLOAT)
    val pitchKey = rebarKey("pitch", RebarSerializers.FLOAT)
    val worldKey = rebarKey("world", RebarSerializers.WORLD)

    override fun getPrimitiveType(): Class<PersistentDataContainer> = PersistentDataContainer::class.java

    override fun getComplexType(): Class<Location> = Location::class.java

    override fun fromPrimitive(primitive: PersistentDataContainer, context: PersistentDataAdapterContext): Location {
        val world = primitive.get(worldKey)
        val x = primitive.get(xKey)!!
        val y = primitive.get(yKey)!!
        val z = primitive.get(zKey)!!
        val yaw = primitive.get(yawKey)!!
        val pitch = primitive.get(pitchKey)!!
        return Location(world, x, y, z, yaw, pitch)
    }

    override fun toPrimitive(complex: Location, context: PersistentDataAdapterContext): PersistentDataContainer {
        val pdc = context.newPersistentDataContainer()
        pdc.set(worldKey, complex.world)
        pdc.set(xKey, complex.x)
        pdc.set(yKey, complex.y)
        pdc.set(zKey,  complex.z)
        pdc.set(yawKey, complex.yaw)
        pdc.set(pitchKey, complex.pitch)
        return pdc
    }
}