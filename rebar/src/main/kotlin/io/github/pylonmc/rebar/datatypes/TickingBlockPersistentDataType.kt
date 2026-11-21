package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.block.interfaces.TickingRebarBlock
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

internal object TickingBlockPersistentDataType : PersistentDataType<PersistentDataContainer, TickingRebarBlock.Companion.TickingBlockData> {
    val tickIntervalKey = rebarKey("tick_interval", RebarSerializers.INTEGER)
    val isAsyncKey = rebarKey("is_async", RebarSerializers.BOOLEAN)

    override fun getPrimitiveType(): Class<PersistentDataContainer> = PersistentDataContainer::class.java

    override fun getComplexType(): Class<TickingRebarBlock.Companion.TickingBlockData> = TickingRebarBlock.Companion.TickingBlockData::class.java

    override fun fromPrimitive(
        primitive: PersistentDataContainer,
        context: PersistentDataAdapterContext
    ): TickingRebarBlock.Companion.TickingBlockData {
        val tickInterval = primitive.get(tickIntervalKey)!!
        val isAsync = primitive.get(isAsyncKey)!!
        return TickingRebarBlock.Companion.TickingBlockData(tickInterval, isAsync, null)
    }

    override fun toPrimitive(
        complex: TickingRebarBlock.Companion.TickingBlockData,
        context: PersistentDataAdapterContext
    ): PersistentDataContainer {
        val pdc = context.newPersistentDataContainer()
        pdc.set(tickIntervalKey, complex.tickInterval)
        pdc.set(isAsyncKey, complex.isAsync)
        return pdc
    }
}