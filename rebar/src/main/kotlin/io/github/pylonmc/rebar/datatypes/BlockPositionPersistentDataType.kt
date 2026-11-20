package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.util.position.BlockPosition
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

object BlockPositionPersistentDataType : PersistentDataType<PersistentDataContainer, BlockPosition> {
    val worldKey = rebarKey("world", RebarSerializers.UUID)
    val xKey = rebarKey("x", RebarSerializers.INTEGER)
    val yKey = rebarKey("y", RebarSerializers.INTEGER)
    val zKey = rebarKey("z", RebarSerializers.INTEGER)

    override fun getPrimitiveType(): Class<PersistentDataContainer> = PersistentDataContainer::class.java

    override fun getComplexType(): Class<BlockPosition> = BlockPosition::class.java

    override fun fromPrimitive(
        primitive: PersistentDataContainer,
        context: PersistentDataAdapterContext
    ): BlockPosition {
        val x = primitive.get(xKey)!!
        val y = primitive.get(yKey)!!
        val z = primitive.get(zKey)!!
        val worldId = primitive.get(worldKey)
        return BlockPosition(worldId, x, y, z)
    }

    override fun toPrimitive(complex: BlockPosition, context: PersistentDataAdapterContext): PersistentDataContainer {
        val pdc = context.newPersistentDataContainer()
        pdc.set(xKey, complex.x)
        pdc.set(yKey, complex.y)
        pdc.set(zKey, complex.z)
        complex.worldId?.let { pdc.set(worldKey, it) }
        return pdc
    }
}