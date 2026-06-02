package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.fluid.FluidWithAmount
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

object FluidWithAmountPersistentDataType : PersistentDataType<PersistentDataContainer, FluidWithAmount> {

    private val fluidKey = rebarKey("fluid")
    private val amountKey = rebarKey("amount")

    override fun getPrimitiveType() = PersistentDataContainer::class.java
    override fun getComplexType() = FluidWithAmount::class.java

    override fun toPrimitive(
        complex: FluidWithAmount,
        context: PersistentDataAdapterContext
    ): PersistentDataContainer {
        val pdc = context.newPersistentDataContainer()
        pdc.set(fluidKey, RebarSerializers.REBAR_FLUID, complex.fluid)
        pdc.set(amountKey, RebarSerializers.DOUBLE, complex.amount)
        return pdc
    }

    override fun fromPrimitive(
        primitive: PersistentDataContainer,
        context: PersistentDataAdapterContext
    ): FluidWithAmount {
        val fluid = primitive.get(fluidKey, RebarSerializers.REBAR_FLUID)!!
        val amount = primitive.get(amountKey, RebarSerializers.DOUBLE)!!
        return FluidWithAmount(fluid, amount)
    }
}