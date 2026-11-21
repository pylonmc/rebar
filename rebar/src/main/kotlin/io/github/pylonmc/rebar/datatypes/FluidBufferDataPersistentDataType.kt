package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.block.interfaces.FluidBufferRebarBlock
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

internal object FluidBufferDataPersistentDataType : PersistentDataType<PersistentDataContainer, FluidBufferRebarBlock.Companion.FluidBufferData> {
    val amountKey = rebarKey("amount", RebarSerializers.DOUBLE)
    val capacityKey = rebarKey("capacity", RebarSerializers.DOUBLE)
    val inputKey = rebarKey("input", RebarSerializers.BOOLEAN)
    val outputKey = rebarKey("output", RebarSerializers.BOOLEAN)

    override fun getPrimitiveType(): Class<PersistentDataContainer> = PersistentDataContainer::class.java

    override fun getComplexType(): Class<FluidBufferRebarBlock.Companion.FluidBufferData> = FluidBufferRebarBlock.Companion.FluidBufferData::class.java

    override fun fromPrimitive(
        primitive: PersistentDataContainer,
        context: PersistentDataAdapterContext
    ): FluidBufferRebarBlock.Companion.FluidBufferData {
        val amount = primitive.get(amountKey)!!
        val capacity = primitive.get(capacityKey)!!
        val input = primitive.get(inputKey)!!
        val output = primitive.get(outputKey)!!
        return FluidBufferRebarBlock.Companion.FluidBufferData(amount, capacity, input, output)
    }

    override fun toPrimitive(
        complex: FluidBufferRebarBlock.Companion.FluidBufferData,
        context: PersistentDataAdapterContext
    ): PersistentDataContainer {
        val pdc = context.newPersistentDataContainer()
        pdc.set(amountKey, complex.amount)
        pdc.set(capacityKey, complex.capacity)
        pdc.set(inputKey, complex.input)
        pdc.set(outputKey, complex.output)
        return pdc
    }
}