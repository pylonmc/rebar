package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.entity.interfaces.TickingRebarEntity
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

internal object TickingEntityPersistentDataType : PersistentDataType<PersistentDataContainer, TickingRebarEntity.Companion.TickingEntityData> {
    val tickIntervalKey = rebarKey("tick_interval")
    val isAsyncKey = rebarKey("is_async")

    override fun getPrimitiveType(): Class<PersistentDataContainer> = PersistentDataContainer::class.java

    override fun getComplexType(): Class<TickingRebarEntity.Companion.TickingEntityData> =
        TickingRebarEntity.Companion.TickingEntityData::class.java

    override fun fromPrimitive(
        primitive: PersistentDataContainer,
        context: PersistentDataAdapterContext
    ): TickingRebarEntity.Companion.TickingEntityData {
        val tickInterval = primitive.get(tickIntervalKey, PersistentDataType.INTEGER)!!
        val isAsync = primitive.get(isAsyncKey, PersistentDataType.BOOLEAN)!!
        return TickingRebarEntity.Companion.TickingEntityData(tickInterval, isAsync, null)
    }

    override fun toPrimitive(
        complex: TickingRebarEntity.Companion.TickingEntityData,
        context: PersistentDataAdapterContext
    ): PersistentDataContainer {
        val pdc = context.newPersistentDataContainer()
        pdc.set(tickIntervalKey, PersistentDataType.INTEGER, complex.tickInterval)
        pdc.set(isAsyncKey, PersistentDataType.BOOLEAN, complex.isAsync)
        return pdc
    }
}