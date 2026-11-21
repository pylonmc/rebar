package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.recipe.ingredient.FluidWithAmount
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

object FluidWithAmountPersistentDataType : PersistentDataType<PersistentDataContainer, FluidWithAmount> {

    private val fluidKey = rebarKey("fluid", RebarSerializers.REBAR_FLUID)
    private val amountKey = rebarKey("amount", RebarSerializers.DOUBLE)

    override fun getPrimitiveType() = PersistentDataContainer::class.java
    override fun getComplexType() = FluidWithAmount::class.java

    override fun toPrimitive(
        complex: FluidWithAmount,
        context: PersistentDataAdapterContext
    ): PersistentDataContainer {
        val pdc = context.newPersistentDataContainer()
        pdc.set(fluidKey, complex.fluid)
        pdc.set(amountKey, complex.amount)
        return pdc
    }

    override fun fromPrimitive(
        primitive: PersistentDataContainer,
        context: PersistentDataAdapterContext
    ): FluidWithAmount {
        val fluid = primitive.get(fluidKey)!!
        val amount = primitive.get(amountKey)!!
        return FluidWithAmount(fluid, amount)
    }
}