package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.util.position.ChunkPosition
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

object ChunkPositionPersistentDataType : PersistentDataType<PersistentDataContainer, ChunkPosition> {
    val xKey = rebarKey("x", RebarSerializers.INTEGER)
    val zKey = rebarKey("z", RebarSerializers.INTEGER)
    val worldKey = rebarKey("world", RebarSerializers.UUID)

    override fun getPrimitiveType(): Class<PersistentDataContainer> = PersistentDataContainer::class.java

    override fun getComplexType(): Class<ChunkPosition> = ChunkPosition::class.java

    override fun fromPrimitive(
        primitive: PersistentDataContainer,
        context: PersistentDataAdapterContext
    ): ChunkPosition {
        val x = primitive.get(xKey)!!
        val z = primitive.get(zKey)!!
        val worldId = primitive.get(worldKey)
        return ChunkPosition(worldId, x, z)
    }

    override fun toPrimitive(complex: ChunkPosition, context: PersistentDataAdapterContext): PersistentDataContainer {
        val pdc = context.newPersistentDataContainer()
        pdc.set(xKey, complex.x)
        pdc.set(zKey, complex.z)
        complex.worldId?.let { pdc.set(worldKey, it) }
        return pdc
    }
}