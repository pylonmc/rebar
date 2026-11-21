package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.block.interfaces.FluidTankRebarBlock
import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.util.setNullable
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

internal object FluidTankDataPersistentDataType : PersistentDataType<PersistentDataContainer, FluidTankRebarBlock.Companion.FluidTankData> {
    val fluidKey = rebarKey("fluid", RebarSerializers.REBAR_FLUID)
    val amountKey = rebarKey("amount", RebarSerializers.DOUBLE)
    val capacityKey = rebarKey("capacity", RebarSerializers.DOUBLE)
    val inputKey = rebarKey("input", RebarSerializers.BOOLEAN)
    val outputKey = rebarKey("output", RebarSerializers.BOOLEAN)

    override fun getPrimitiveType(): Class<PersistentDataContainer> = PersistentDataContainer::class.java

    override fun getComplexType(): Class<FluidTankRebarBlock.Companion.FluidTankData> = FluidTankRebarBlock.Companion.FluidTankData::class.java

    override fun fromPrimitive(
        primitive: PersistentDataContainer,
        context: PersistentDataAdapterContext
    ): FluidTankRebarBlock.Companion.FluidTankData {
        val fluid = primitive.get(fluidKey)
        val amount = primitive.get(amountKey)!!
        val capacity = primitive.get(capacityKey)!!
        val input = primitive.get(inputKey)!!
        val output = primitive.get(outputKey)!!
        return FluidTankRebarBlock.Companion.FluidTankData(fluid, amount, capacity, input, output)
    }

    override fun toPrimitive(
        complex: FluidTankRebarBlock.Companion.FluidTankData,
        context: PersistentDataAdapterContext
    ): PersistentDataContainer {
        val pdc = context.newPersistentDataContainer()
        pdc.setNullable(fluidKey, complex.fluid)
        pdc.set(amountKey, complex.amount)
        pdc.set(capacityKey, complex.capacity)
        pdc.set(inputKey, complex.input)
        pdc.set(outputKey, complex.output)
        return pdc
    }
}