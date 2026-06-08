package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.block.interfaces.SimpleRebarMultiblock
import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.util.setNullable
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

internal object SimpleMultiblockDataPersistentDataType : PersistentDataType<PersistentDataContainer, SimpleRebarMultiblock.Companion.SimpleMultiblockData> {
    val facingKey = rebarKey("facing")

    override fun getPrimitiveType(): Class<PersistentDataContainer> = PersistentDataContainer::class.java

    override fun getComplexType(): Class<SimpleRebarMultiblock.Companion.SimpleMultiblockData> = SimpleRebarMultiblock.Companion.SimpleMultiblockData::class.java

    override fun fromPrimitive(
        primitive: PersistentDataContainer,
        context: PersistentDataAdapterContext
    ): SimpleRebarMultiblock.Companion.SimpleMultiblockData {
        val facing = primitive.get(facingKey, RebarSerializers.BLOCK_FACE)
        return SimpleRebarMultiblock.Companion.SimpleMultiblockData(facing)
    }

    override fun toPrimitive(
        complex: SimpleRebarMultiblock.Companion.SimpleMultiblockData,
        context: PersistentDataAdapterContext
    ): PersistentDataContainer {
        val pdc = context.newPersistentDataContainer()
        pdc.setNullable(facingKey, RebarSerializers.BLOCK_FACE, complex.direction)
        return pdc
    }
}