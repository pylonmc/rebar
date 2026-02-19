package io.github.pylonmc.rebar.content.fluid

import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.entity.RebarEntity
import io.github.pylonmc.rebar.entity.base.RebarDeathEntity
import io.github.pylonmc.rebar.entity.display.ItemDisplayBuilder
import io.github.pylonmc.rebar.entity.display.transform.TransformBuilder
import io.github.pylonmc.rebar.event.RebarEntityDeathEvent
import io.github.pylonmc.rebar.fluid.FluidManager
import io.github.pylonmc.rebar.fluid.FluidPointType
import io.github.pylonmc.rebar.fluid.VirtualFluidPoint
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.util.setNullable
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.ItemDisplay
import org.bukkit.event.EventPriority
import org.bukkit.persistence.PersistentDataContainer
import java.util.UUID
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * A 'endpoint display' is one of the red/green displays that indicates a block's fluid input/output.
 */
class FluidEndpointDisplay : RebarEntity<ItemDisplay>, RebarDeathEntity, FluidPointDisplay {
    override val point: VirtualFluidPoint
    var connectedPipeDisplay: UUID?
    override val connectedPipeDisplays: Set<UUID>
        get() = setOfNotNull(connectedPipeDisplay)
    val face: BlockFace
    val radius: Float
    val pipeDisplay
        get() = connectedPipeDisplay?.let { EntityStorage.getAs<FluidPipeDisplay>(it) }

    constructor(block: Block, type: FluidPointType, face: BlockFace, radius: Float = 0.5F) : super(KEY, makeEntity(block, type, face, radius)) {
        this.connectedPipeDisplay = null
        this.point = VirtualFluidPoint(block, type)
        this.face = face
        this.radius = radius

        FluidManager.add(point)
        EntityStorage.add(this)
    }

    @Suppress("unused")
    constructor(entity: ItemDisplay) : super(entity) {
        val pdc = entity.persistentDataContainer

        this.connectedPipeDisplay = pdc.get(CONNECTED_PIPE_DISPLAY_KEY, RebarSerializers.UUID)
        this.point = pdc.get(CONNECTION_POINT_KEY, RebarSerializers.FLUID_CONNECTION_POINT)!!
        this.face = pdc.get(FACE_KEY, RebarSerializers.BLOCK_FACE)!!
        this.radius = pdc.get(RADIUS_KEY, RebarSerializers.FLOAT)!!

        FluidManager.add(point)
    }

    override fun write(pdc: PersistentDataContainer) {
        pdc.setNullable(CONNECTED_PIPE_DISPLAY_KEY, RebarSerializers.UUID, connectedPipeDisplay)
        pdc.set(CONNECTION_POINT_KEY, RebarSerializers.FLUID_CONNECTION_POINT, point)
        pdc.set(FACE_KEY, RebarSerializers.BLOCK_FACE, face)
        pdc.set(RADIUS_KEY, RebarSerializers.FLOAT, radius)
    }

    override fun connectPipeDisplay(uuid: UUID) {
        this.connectedPipeDisplay = uuid
    }

    override fun disconnectPipeDisplay(uuid: UUID) {
        check(this.connectedPipeDisplay == uuid) { "$uuid is not connected" }
        this.connectedPipeDisplay = null
    }

    override fun onDeath(event: RebarEntityDeathEvent, priority: EventPriority) {
        pipeDisplay?.delete(null, null)
        FluidManager.remove(point)
    }

    override fun onUnload() {
        FluidManager.unload(point)
    }

    companion object {
        const val POINT_SIZE: Float = 0.12f

        @JvmField
        val distanceFromFluidPointCenterToCorner = sqrt(3 * (POINT_SIZE / 2.0F).pow(2))

        @JvmField
        val KEY = rebarKey("fluid_pipe_endpoint_display")

        private val CONNECTED_PIPE_DISPLAY_KEY = rebarKey("connected_pipe_display")
        private val CONNECTION_POINT_KEY = rebarKey("connection_point")
        private val FACE_KEY = rebarKey("face")
        private val RADIUS_KEY = rebarKey("radius")

        private fun makeEntity(block: Block, type: FluidPointType, face: BlockFace, radius: Float = 0.5F): ItemDisplay {
            return ItemDisplayBuilder()
                .transformation(TransformBuilder()
                    .scale(POINT_SIZE)
                )
                .itemStack(ItemStackBuilder.of(type.material)
                    .addCustomModelDataString("fluid_point_display:${type.name.lowercase()}")
                )
                // add a little bit to ensure the point is not obscured by the block itself
                .build(block.location.toCenterLocation().add(face.direction.multiply(radius + 0.001)))
        }
    }
}
