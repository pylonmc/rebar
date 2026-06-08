package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.block.interfaces.FluidTankRebarBlock
import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.util.setNullable
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

internal object FluidTankDataPersistentDataType : PersistentDataType<PersistentDataContainer, FluidTankRebarBlock.Companion.FluidTankData> {
    val fluidKey = rebarKey("fluid")
    val amountKey = rebarKey("amount")
    val capacityKey = rebarKey("capacity")
    val inputKey = rebarKey("input")
    val outputKey = rebarKey("output")

    override fun getPrimitiveType(): Class<PersistentDataContainer> = PersistentDataContainer::class.java

    override fun getComplexType(): Class<FluidTankRebarBlock.Companion.FluidTankData> = FluidTankRebarBlock.Companion.FluidTankData::class.java

    override fun fromPrimitive(
        primitive: PersistentDataContainer,
        context: PersistentDataAdapterContext
    ): FluidTankRebarBlock.Companion.FluidTankData {
        val fluid = primitive.get(fluidKey, RebarSerializers.REBAR_FLUID)
        val amount = primitive.get(amountKey, RebarSerializers.DOUBLE)!!
        val capacity = primitive.get(capacityKey, RebarSerializers.DOUBLE)!!
        val input = primitive.get(inputKey, RebarSerializers.BOOLEAN)!!
        val output = primitive.get(outputKey, RebarSerializers.BOOLEAN)!!
        return FluidTankRebarBlock.Companion.FluidTankData(fluid, amount, capacity, input, output)
    }

    override fun toPrimitive(
        complex: FluidTankRebarBlock.Companion.FluidTankData,
        context: PersistentDataAdapterContext
    ): PersistentDataContainer {
        val pdc = context.newPersistentDataContainer()
        pdc.setNullable(fluidKey, RebarSerializers.REBAR_FLUID, complex.fluid)
        pdc.set(amountKey, RebarSerializers.DOUBLE, complex.amount)
        pdc.set(capacityKey, RebarSerializers.DOUBLE, complex.capacity)
        pdc.set(inputKey, RebarSerializers.BOOLEAN, complex.input)
        pdc.set(outputKey, RebarSerializers.BOOLEAN, complex.output)
        return pdc
    }
}