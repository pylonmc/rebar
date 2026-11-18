package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.block.interfaces.CargoRebarBlock
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

internal object CargoBlockPersistentDataType : PersistentDataType<PersistentDataContainer, CargoRebarBlock.Companion.CargoBlockData> {
    val groupsKey = rebarKey("groups", RebarSerializers.MAP.mapTypeFrom(RebarSerializers.BLOCK_FACE, RebarSerializers.STRING))
    val transferRateKey = rebarKey("transfer_rate", RebarSerializers.INTEGER)

    override fun getPrimitiveType(): Class<PersistentDataContainer> = PersistentDataContainer::class.java

    override fun getComplexType(): Class<CargoRebarBlock.Companion.CargoBlockData> = CargoRebarBlock.Companion.CargoBlockData::class.java

    override fun fromPrimitive(
        primitive: PersistentDataContainer,
        context: PersistentDataAdapterContext
    ): CargoRebarBlock.Companion.CargoBlockData {
        val groups = primitive.get(groupsKey)!!.toMutableMap()
        val transferRate = primitive.get(transferRateKey)!!
        return CargoRebarBlock.Companion.CargoBlockData(groups, transferRate)
    }

    override fun toPrimitive(
        complex: CargoRebarBlock.Companion.CargoBlockData,
        context: PersistentDataAdapterContext
    ): PersistentDataContainer {
        val pdc = context.newPersistentDataContainer()
        pdc.set(groupsKey, complex.groups)
        pdc.set(transferRateKey, complex.transferRate)
        return pdc
    }
}