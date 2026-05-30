package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.block.interfaces.CargoRebarBlock
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

internal object CargoBlockPersistentDataType : PersistentDataType<PersistentDataContainer, CargoRebarBlock.Companion.CargoBlockData> {
    val groupsKey = rebarKey("groups")
    val groupsType = RebarSerializers.MAP.mapTypeFrom(RebarSerializers.BLOCK_FACE, RebarSerializers.STRING)
    val transferRateKey = rebarKey("transfer_rate")

    override fun getPrimitiveType(): Class<PersistentDataContainer> = PersistentDataContainer::class.java

    override fun getComplexType(): Class<CargoRebarBlock.Companion.CargoBlockData> = CargoRebarBlock.Companion.CargoBlockData::class.java

    override fun fromPrimitive(
        primitive: PersistentDataContainer,
        context: PersistentDataAdapterContext
    ): CargoRebarBlock.Companion.CargoBlockData {
        val groups = primitive.get(groupsKey, groupsType)!!.toMutableMap()
        val transferRate = primitive.get(transferRateKey, RebarSerializers.INTEGER)!!
        return CargoRebarBlock.Companion.CargoBlockData(groups, transferRate)
    }

    override fun toPrimitive(
        complex: CargoRebarBlock.Companion.CargoBlockData,
        context: PersistentDataAdapterContext
    ): PersistentDataContainer {
        val pdc = context.newPersistentDataContainer()
        pdc.set(groupsKey, groupsType, complex.groups)
        pdc.set(transferRateKey, RebarSerializers.INTEGER, complex.transferRate)
        return pdc
    }
}