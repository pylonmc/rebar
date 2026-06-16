package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.processor.RebarProcessor
import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.util.setNullable
import org.bukkit.Bukkit
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

object ProcessorPersistentDataType : PersistentDataType<PersistentDataContainer, RebarProcessor> {
    val durationTicksKey = rebarKey("duration_ticks")
    val remainingTicksKey = rebarKey("remaining_ticks")

    override fun getPrimitiveType(): Class<PersistentDataContainer> = PersistentDataContainer::class.java

    override fun getComplexType(): Class<RebarProcessor> = RebarProcessor::class.java

    override fun fromPrimitive(
        primitive: PersistentDataContainer,
        context: PersistentDataAdapterContext
    ): RebarProcessor {
        val durationTicks = primitive.get(durationTicksKey, RebarSerializers.INTEGER)
        val remainingTicks = primitive.get(remainingTicksKey, RebarSerializers.INTEGER)
        val processor = RebarProcessor()
        if (durationTicks != null && remainingTicks != null) {
            processor.process = RebarProcessor.RebarProcess(durationTicks, remainingTicks)
        }
        return processor
    }

    override fun toPrimitive(complex: RebarProcessor, context: PersistentDataAdapterContext): PersistentDataContainer {
        val pdc = context.newPersistentDataContainer()
        pdc.setNullable(durationTicksKey, RebarSerializers.INTEGER, complex.durationTicks)
        pdc.setNullable(remainingTicksKey, RebarSerializers.INTEGER, complex.remainingTicks)
        return pdc
    }
}