package io.github.pylonmc.rebar.content.fluid

import io.github.pylonmc.rebar.block.BlockStorage
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
import io.github.pylonmc.rebar.util.IMMEDIATE_FACES
import io.github.pylonmc.rebar.util.rebarKey
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.CustomModelData
import org.bukkit.block.Block
import org.bukkit.entity.ItemDisplay
import org.bukkit.event.EventPriority
import org.bukkit.persistence.PersistentDataContainer
import java.util.*

/**
 * A 'intersection display' is one of the gray displays that indicates one or more pipes being joined together.
 */
class FluidIntersectionDisplay : RebarEntity<ItemDisplay>, RebarDeathEntity, FluidPointDisplay {
    override val point: VirtualFluidPoint
    override val connectedPipeDisplays: MutableSet<UUID>

    constructor(block: Block) : super(KEY, makeEntity(block)) {
        this.connectedPipeDisplays = mutableSetOf()
        this.point = VirtualFluidPoint(block, FluidPointType.INTERSECTION)
        EntityStorage.add(this)
        FluidManager.add(point)
    }

    @Suppress("unused")
    constructor(entity: ItemDisplay) : super(entity) {
        val pdc = entity.persistentDataContainer

        this.connectedPipeDisplays = pdc.get(CONNECTED_PIPE_DISPLAYS_KEY, CONNECTED_PIPE_DISPLAYS_TYPE)!!.toMutableSet()
        this.point = pdc.get(CONNECTION_POINT_KEY, RebarSerializers.FLUID_CONNECTION_POINT)!!

        FluidManager.add(point)
    }

    override fun write(pdc: PersistentDataContainer) {
        pdc.set(CONNECTED_PIPE_DISPLAYS_KEY, CONNECTED_PIPE_DISPLAYS_TYPE, connectedPipeDisplays)
        pdc.set(CONNECTION_POINT_KEY, RebarSerializers.FLUID_CONNECTION_POINT, point)
    }

    override fun writeDebugInfo(pdc: PersistentDataContainer) {
        pdc.set(CONNECTION_POINT_KEY, RebarSerializers.FLUID_CONNECTION_POINT, point)
        val connectedPdc = pdc.adapterContext.newPersistentDataContainer()
        for (connectedPipeId in connectedPipeDisplays) {
            val connectedPipe = EntityStorage.getAs<FluidPipeDisplay>(connectedPipeId)
            if (connectedPipe == null) {
                connectedPdc.set(rebarKey(connectedPipeId.toString()), RebarSerializers.STRING, "MISSING")
                continue
            }
            val pipePdc = pdc.adapterContext.newPersistentDataContainer()
            connectedPipe.writeDebugInfo(pipePdc)
            connectedPdc.set(rebarKey(connectedPipeId.toString()), RebarSerializers.TAG_CONTAINER, pipePdc)
        }
        pdc.set(CONNECTED_PIPE_DISPLAYS_KEY, RebarSerializers.TAG_CONTAINER, connectedPdc)
    }

    @Suppress("UnstableApiUsage")
    fun updateItemDisplay() {
        if (connectedPipeDisplays.isEmpty()) return

        val marker = BlockStorage.getAs(FluidIntersectionMarker::class.java, entity.location.block) ?: return
        val modelData = CustomModelData.customModelData()
        modelData.addString("fluid_point_intersection:${marker.pipe.key}")

        val from = this.entity.location
        for (face in IMMEDIATE_FACES) {
            var hasFace = false
            for (displayId in this.connectedPipeDisplays) {
                val display = EntityStorage.getAs(FluidPipeDisplay::class.java, displayId) ?: continue
                val towards = display.entity.location.subtract(from).toVector().normalize()
                if (face.direction == towards) {
                    hasFace = true
                    break
                }
            }
            modelData.addString("${face.name.lowercase()}=$hasFace")
        }
        this.entity.setItemStack(this.entity.itemStack.apply {
            setData(DataComponentTypes.CUSTOM_MODEL_DATA, modelData)
        })
    }

    override fun connectPipeDisplay(uuid: UUID) {
        this.connectedPipeDisplays.add(uuid)
        updateItemDisplay()
    }

    override fun disconnectPipeDisplay(uuid: UUID) {
        check(uuid in this.connectedPipeDisplays) { "$uuid is not connected" }
        this.connectedPipeDisplays.remove(uuid)
        updateItemDisplay()
    }

    override fun onDeath(event: RebarEntityDeathEvent, priority: EventPriority) {
        FluidManager.remove(point)
    }

    override fun onUnload() {
        FluidManager.unload(point)
    }

    companion object {
        @JvmStatic
        val KEY = rebarKey("fluid_pipe_intersection_display")

        private val CONNECTED_PIPE_DISPLAYS_KEY = rebarKey("connected_pipe_displays")
        private val CONNECTED_PIPE_DISPLAYS_TYPE = RebarSerializers.SET.setTypeFrom(RebarSerializers.UUID)
        private val CONNECTION_POINT_KEY = rebarKey("connection_point")

        @JvmSynthetic
        internal fun makeEntity(block: Block): ItemDisplay {
            return ItemDisplayBuilder()
                .transformation(TransformBuilder()
                    .scale(FluidEndpointDisplay.POINT_SIZE)
                )
                .itemStack(ItemStackBuilder.of(FluidPointType.INTERSECTION.material)
                    .addCustomModelDataString("fluid_point_intersection:none")
                )
                .itemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED)
                .build(block.location.toCenterLocation())
        }
    }
}