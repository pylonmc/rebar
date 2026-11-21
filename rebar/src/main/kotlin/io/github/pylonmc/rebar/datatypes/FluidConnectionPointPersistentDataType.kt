package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.fluid.FluidPointType
import io.github.pylonmc.rebar.fluid.VirtualFluidPoint
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

object FluidConnectionPointPersistentDataType : PersistentDataType<PersistentDataContainer, VirtualFluidPoint> {

    private val ID_KEY = rebarKey("id", RebarSerializers.UUID)
    private val POSITION_KEY = rebarKey("position", RebarSerializers.BLOCK_POSITION)
    private val TYPE_KEY = rebarKey("type", RebarSerializers.STRING)
    private val CONNECTED_POINTS_KEY = rebarKey("connected_points", RebarSerializers.SET.setTypeFrom(RebarSerializers.UUID))

    override fun getPrimitiveType(): Class<PersistentDataContainer> = PersistentDataContainer::class.java

    override fun getComplexType(): Class<VirtualFluidPoint> = VirtualFluidPoint::class.java

    override fun fromPrimitive(primitive: PersistentDataContainer, context: PersistentDataAdapterContext): VirtualFluidPoint {
        return VirtualFluidPoint(
            primitive.get(ID_KEY)!!,
            primitive.get(POSITION_KEY)!!,
            FluidPointType.valueOf(primitive.get(TYPE_KEY)!!),
            primitive.get(CONNECTED_POINTS_KEY)!!.toMutableSet()
        )
    }

    override fun toPrimitive(complex: VirtualFluidPoint, context: PersistentDataAdapterContext): PersistentDataContainer {
        val pdc = context.newPersistentDataContainer()
        pdc.set(ID_KEY, complex.id)
        pdc.set(POSITION_KEY, complex.position)
        pdc.set(TYPE_KEY, complex.type.name)
        pdc.set(CONNECTED_POINTS_KEY, complex.connectedPoints)
        // Segment is intentionally not persisted as the segment is decided at runtime
        return pdc
    }
}