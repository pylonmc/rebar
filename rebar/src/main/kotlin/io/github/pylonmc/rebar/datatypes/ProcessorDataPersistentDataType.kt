package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.block.interfaces.ProcessorRebarBlock
import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.util.setNullable
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

internal object ProcessorDataPersistentDataType : PersistentDataType<PersistentDataContainer, ProcessorRebarBlock.ProcessorData> {

    private val PROCESS_TIME_TICKS_KEY = rebarKey("total_process_ticks")
    private val PROCESS_TICKS_REMAINING_KEY = rebarKey("process_ticks_remaining")
    private val PROGRESS_ITEM_KEY = rebarKey("progress_item")

    override fun getPrimitiveType(): Class<PersistentDataContainer> = PersistentDataContainer::class.java

    override fun getComplexType(): Class<ProcessorRebarBlock.ProcessorData> = ProcessorRebarBlock.ProcessorData::class.java

    override fun fromPrimitive(primitive: PersistentDataContainer, context: PersistentDataAdapterContext): ProcessorRebarBlock.ProcessorData {
        return ProcessorRebarBlock.ProcessorData(
            primitive.get(PROCESS_TIME_TICKS_KEY, RebarSerializers.INTEGER),
            primitive.get(PROCESS_TICKS_REMAINING_KEY, RebarSerializers.INTEGER),
            primitive.get(PROGRESS_ITEM_KEY, RebarSerializers.PROGRESS_ITEM),
        )
    }

    override fun toPrimitive(complex: ProcessorRebarBlock.ProcessorData, context: PersistentDataAdapterContext): PersistentDataContainer {
        val pdc = context.newPersistentDataContainer()
        pdc.setNullable(PROCESS_TIME_TICKS_KEY, RebarSerializers.INTEGER, complex.processTimeTicks)
        pdc.setNullable(PROCESS_TICKS_REMAINING_KEY, RebarSerializers.INTEGER, complex.processTicksRemaining)
        pdc.setNullable(PROGRESS_ITEM_KEY, RebarSerializers.PROGRESS_ITEM, complex.progressItem)
        return pdc
    }
}
