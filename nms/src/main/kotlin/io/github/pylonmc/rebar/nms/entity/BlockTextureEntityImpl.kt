package io.github.pylonmc.rebar.nms.entity

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.culling.BlockCullingEngine
import io.github.pylonmc.rebar.entity.display.transform.TransformUtil.toMatrix
import io.github.pylonmc.rebar.entity.packet.BlockTextureEntity
import io.github.pylonmc.rebar.entity.packet.BlockTextureEntity.Companion.BLOCK_OVERLAP_INCREASE
import io.github.pylonmc.rebar.entity.packet.BlockTextureEntity.Companion.MAX_SCALE_INCREASE
import io.github.pylonmc.rebar.entity.packet.BlockTextureEntity.Companion.SCALE_DISTANCE_INCREASE
import io.github.pylonmc.rebar.entity.packet.BlockTextureEntity.Companion.SCALE_DISTANCE_THRESHOLD
import io.github.pylonmc.rebar.util.IMMEDIATE_FACES
import io.github.pylonmc.rebar.util.delayTicks
import io.github.pylonmc.rebar.util.position.BlockPosition
import kotlinx.coroutines.launch
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.*
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SyncedDataHolder
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.util.LightCoordsUtil
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.PositionMoveRotation
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.block.BlockFace
import org.bukkit.craftbukkit.block.CraftBlock
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Display
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Transformation
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.abs
import kotlin.math.min
import com.mojang.math.Transformation as NmsTransformation
import net.minecraft.world.entity.Display as NmsDisplay
import net.minecraft.world.item.ItemStack as NmsItemStack

class BlockTextureEntityImpl : BlockTextureEntity, SyncedDataHolder {
    override val block: RebarBlock
    val neighborPositions: List<Long>
    override val lightDelegatePositions: List<Long>
        get() = if (directLighting) emptyList() else neighborPositions
    override var id: Int
    override var uuid: UUID
    override val viewers: MutableSet<UUID> = mutableSetOf()

    var refreshesFrozen: Boolean = false
    var refreshesFrozenExpireTime: Long = 0

    private val position: Vec3
        get() = Vec3(block.block.x + 0.5 + lightingOffset.x, block.block.y + 0.5 + lightingOffset.y, block.block.z + 0.5 + lightingOffset.z)
    override var isSpawned: Boolean = false

    private val entityData: SynchedEntityData

    /**
     * The data to sync on player refresh
     */
    private val refreshData: List<SynchedEntityData.DataItem<*>>

    /**
     * The data to sync when lighting offset changes
     */
    private val lightingData: List<SynchedEntityData.DataItem<*>>

    /**
     * The data for the item of the [BlockTextureEntity]
     */
    internal val itemUpdateData: SynchedEntityData.DataItem<NmsItemStack>

    /**
     * The packet used to update only the item of the [BlockTextureEntity]
     */
    internal val itemUpdatePacket: ClientboundSetEntityDataPacket
        get() = ClientboundSetEntityDataPacket(id, listOf(itemUpdateData.value()))

    /**
     * If the brightness has been overridden externally or the block state already has lighting data,
     * then we don't need to delegate to a neighboring block to get lighting data
     */
    private val directLighting: Boolean
        get() = brightness != null || lastState.hasLightingData

    /**
     * The offset needed to put the entity in a location that has lighting data
     * This is needed so that occluding blocks don't cause the entity to be rendered at 0 brightness
     */
    private var lightingOffset: Vector3f = ZERO_VECTOR
    private var lightingUpdateCooldown: Long = 0
    private var lightingUpdateScheduled: Boolean = false

    /**
     * The transformation of the texture entity needs to be centered on the bounding box of the block instead of the block center.
     * This is needed so that the scaling of non-full blocks (like slabs) scales around the correct point to combat z-fighting.
     */
    private var centerOffset: Vector3f

    var lastState: BlockState
    var stateUpdated: Boolean = false

    /**
     * A map of the last scale increase sent to each player, used to prevent sending unnecessary scale updates when the player is far enough away that the scale increase doesn't change anymore or when the change is by to small a margin
     */
    private val lastScaleIncreases: MutableMap<UUID, Float> = mutableMapOf()

    constructor(block: RebarBlock) {
        this.block = block
        this.neighborPositions = IMMEDIATE_FACES.map { BlockPosition.asLong(block.block.getRelative(it)) }
        this.id = Bukkit.getUnsafe().nextEntityId()
        this.uuid = Mth.createInsecureUUID(Entity.SHARED_RANDOM)
        this.entityData = EntityDataAccess.fakedDataBuilder(this, NmsDisplay.ItemDisplay::class.java).apply {
            // An ItemDisplay's class hierarchy is Entity -> Display -> ItemDisplay, so to fake it we need to define all the data parameters for all 3 classes identically
            define(EntityDataAccess.ENTITY_DATA_SHARED_FLAGS_ID, 0.toByte())
            define(EntityDataAccess.ENTITY_DATA_AIR_SUPPLY_ID, 300)
            define(EntityDataAccess.ENTITY_DATA_CUSTOM_NAME_VISIBLE, false)
            define(EntityDataAccess.ENTITY_DATA_CUSTOM_NAME, Optional.empty())
            define(EntityDataAccess.ENTITY_DATA_SILENT, false)
            define(EntityDataAccess.ENTITY_DATA_NO_GRAVITY, false)
            define(EntityDataAccess.ENTITY_DATA_POSE, Pose.STANDING)
            define(EntityDataAccess.ENTITY_DATA_TICKS_FROZEN, 0)

            define(EntityDataAccess.DISPLAY_DATA_POS_ROT_INTERPOLATION_DURATION_ID, 0)
            define(EntityDataAccess.DISPLAY_DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID, 0)
            define(EntityDataAccess.DISPLAY_DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID, 0)
            define(EntityDataAccess.DISPLAY_DATA_TRANSLATION_ID, ZERO_VECTOR)
            define(EntityDataAccess.DISPLAY_DATA_SCALE_ID, Vector3f(1.0F, 1.0F, 1.0F))
            define(EntityDataAccess.DISPLAY_DATA_RIGHT_ROTATION_ID, ZERO_QUATERNION)
            define(EntityDataAccess.DISPLAY_DATA_LEFT_ROTATION_ID, ZERO_QUATERNION)
            define(EntityDataAccess.DISPLAY_DATA_BILLBOARD_RENDER_CONSTRAINTS_ID, 0.toByte())
            define(EntityDataAccess.DISPLAY_DATA_BRIGHTNESS_OVERRIDE_ID, -1)
            define(EntityDataAccess.DISPLAY_DATA_VIEW_RANGE_ID, 1.0F)
            define(EntityDataAccess.DISPLAY_DATA_SHADOW_RADIUS_ID, 0.0F)
            define(EntityDataAccess.DISPLAY_DATA_SHADOW_STRENGTH_ID, 1.0F)
            define(EntityDataAccess.DISPLAY_DATA_WIDTH_ID, 0.0F)
            define(EntityDataAccess.DISPLAY_DATA_HEIGHT_ID, 0.0F)
            define(EntityDataAccess.DISPLAY_DATA_GLOW_COLOR_OVERRIDE_ID, -1)

            define(EntityDataAccess.ITEM_DISPLAY_DATA_ITEM_STACK_ID, NmsItemStack.EMPTY)
            define(EntityDataAccess.ITEM_DISPLAY_DATA_ITEM_DISPLAY_ID, ItemDisplayContext.NONE.id)
        }.build()
        this.entityData.set(EntityDataAccess.DISPLAY_DATA_SCALE_ID, Vector3f(1.0F + BLOCK_OVERLAP_INCREASE))
        this.entityData.set(EntityDataAccess.DISPLAY_DATA_POS_ROT_INTERPOLATION_DURATION_ID, 1)

        this.refreshData = REFRESH_IDS.map { entityData.getItem(it) }
        this.lightingData = REFRESH_IDS.map { entityData.getItem(it) } + INTERPOLATION_IDS.map { entityData.getItem(it) }
        this.itemUpdateData = entityData.getItem(EntityDataAccess.ITEM_DISPLAY_DATA_ITEM_STACK_ID)
        this.lastState = (block.block as CraftBlock).blockState
        this.lightingOffset = calculateLightingOffset()
        this.centerOffset = calculateCenterOffset()
        tryUpdateLighting()
    }

    val transformation: Transformation
        get() = Transformation(
            Vector3f(entityData.get(EntityDataAccess.DISPLAY_DATA_TRANSLATION_ID)),
            ZERO_QUATERNION,
            Vector3f(entityData.get(EntityDataAccess.DISPLAY_DATA_SCALE_ID)),
            ZERO_QUATERNION
        )
    val transformationMatrix: Matrix4f
        get() = transformation.toMatrix()

    override var shadowRadius: Float
        get() = entityData.get(EntityDataAccess.DISPLAY_DATA_SHADOW_RADIUS_ID)
        set(value) = entityData.set(EntityDataAccess.DISPLAY_DATA_SHADOW_RADIUS_ID, value)
    override var shadowStrength: Float
        get() = entityData.get(EntityDataAccess.DISPLAY_DATA_SHADOW_STRENGTH_ID)
        set(value) = entityData.set(EntityDataAccess.DISPLAY_DATA_SHADOW_STRENGTH_ID, value)

    override var displayWidth: Float
        get() = entityData.get(EntityDataAccess.DISPLAY_DATA_WIDTH_ID)
        set(value) = entityData.set(EntityDataAccess.DISPLAY_DATA_WIDTH_ID, value)
    override var displayHeight: Float
        get() = entityData.get(EntityDataAccess.DISPLAY_DATA_HEIGHT_ID)
        set(value) = entityData.set(EntityDataAccess.DISPLAY_DATA_HEIGHT_ID, value)
    override var viewRange: Float
        get() = entityData.get(EntityDataAccess.DISPLAY_DATA_VIEW_RANGE_ID)
        set(value) = entityData.set(EntityDataAccess.DISPLAY_DATA_VIEW_RANGE_ID, value)

    override var glowColorOverride: Color?
        get() = when(val color = entityData.get(EntityDataAccess.DISPLAY_DATA_GLOW_COLOR_OVERRIDE_ID)) {
            -1 -> null
            else -> Color.fromRGB(color and 16777215)
        }
        set(value) = entityData.set(EntityDataAccess.DISPLAY_DATA_GLOW_COLOR_OVERRIDE_ID, value?.asRGB() ?: -1)
    override var billboard: Display.Billboard
        get() = Display.Billboard.entries[entityData.get(EntityDataAccess.DISPLAY_DATA_BILLBOARD_RENDER_CONSTRAINTS_ID).toInt()]
        set(value) = entityData.set(EntityDataAccess.DISPLAY_DATA_BILLBOARD_RENDER_CONSTRAINTS_ID, NmsDisplay.BillboardConstraints.valueOf(value.name).ordinal.toByte())
    override var brightness: Display.Brightness?
        get() = when (val brightness = entityData.get(EntityDataAccess.DISPLAY_DATA_BRIGHTNESS_OVERRIDE_ID)) {
            -1 -> null
            else -> Display.Brightness(LightCoordsUtil.block(brightness), LightCoordsUtil.sky(brightness))
        }
        set(value) = entityData.set(EntityDataAccess.DISPLAY_DATA_BRIGHTNESS_OVERRIDE_ID, value?.let { LightCoordsUtil.pack(value.blockLight, value.skyLight) } ?: -1)

    override var itemStack: ItemStack?
        get() = entityData.get(EntityDataAccess.ITEM_DISPLAY_DATA_ITEM_STACK_ID).asBukkitCopy()
        set(value) = entityData.set(EntityDataAccess.ITEM_DISPLAY_DATA_ITEM_STACK_ID, (value as? CraftItemStack)?.handle ?: NmsItemStack.EMPTY)
    override var itemDisplayTransform: ItemDisplay.ItemDisplayTransform
        get() = ItemDisplay.ItemDisplayTransform.entries[entityData.get(EntityDataAccess.ITEM_DISPLAY_DATA_ITEM_DISPLAY_ID).toInt()]
        set(value) = entityData.set(EntityDataAccess.ITEM_DISPLAY_DATA_ITEM_DISPLAY_ID, value.ordinal.toByte())

    override fun onSyncedDataUpdated(p0: EntityDataAccessor<*>) {}
    override fun onSyncedDataUpdated(p0: List<SynchedEntityData.DataValue<*>>) {}

    private fun createDataPacket(playerId: UUID, distanceSquared: Double, trackedData: List<SynchedEntityData.DataItem<*>>, optional: Boolean, forceTickDelay: Boolean = false)
        = createDataPacketRaw(playerId, distanceSquared, trackedData.map { it.value() }, optional, forceTickDelay)

    private fun createDataPacketRaw(playerId: UUID, distanceSquared: Double, trackedData: List<SynchedEntityData.DataValue<*>>, optional: Boolean, forceTickDelay: Boolean = false) : ClientboundSetEntityDataPacket? {
        val scaleIncrease = min((distanceSquared * SCALE_DISTANCE_INCREASE).toFloat(), MAX_SCALE_INCREASE)
        val lastScaleIncrease = lastScaleIncreases[playerId] ?: 0f
        if (abs(scaleIncrease - lastScaleIncrease) < SCALE_DISTANCE_THRESHOLD && optional) {
            return null
        }

        lastScaleIncreases[playerId] = scaleIncrease

        val matrix = transformationMatrix
            .scaleLocal(1 + scaleIncrease)
            .translateLocal(centerOffset.mul(scaleIncrease, Vector3f()).negate())
            .translateLocal(lightingOffset.negate(Vector3f()))
        val transformation = NmsTransformation(matrix)

        return ClientboundSetEntityDataPacket(id, trackedData.map { data ->
            when(data.id) {
                EntityDataAccess.DISPLAY_DATA_TRANSLATION_ID.id -> SynchedEntityData.DataValue(data.id, EntityDataSerializers.VECTOR3, transformation.translation())
                EntityDataAccess.DISPLAY_DATA_SCALE_ID.id -> SynchedEntityData.DataValue(data.id, EntityDataSerializers.VECTOR3, transformation.scale())
                EntityDataAccess.DISPLAY_DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID.id -> if (forceTickDelay) SynchedEntityData.DataValue(data.id, EntityDataSerializers.INT, 1) else data
                EntityDataAccess.DISPLAY_DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID.id -> if (forceTickDelay) SynchedEntityData.DataValue(data.id, EntityDataSerializers.INT, 0) else data
                else -> data
            }
        })
    }

    private fun sendPacket(playerId: UUID, packet: Packet<*>) {
        val player = (Bukkit.getPlayer(playerId) as? CraftPlayer)?.handle ?: return
        player.connection.send(packet)
    }

    private fun addViewer(playerId: UUID, distanceSquared: Double) {
        sendPacket(playerId, ClientboundAddEntityPacket(
            id, uuid, position.x, position.y, position.z, 0f, 0f,
            EntityType.ITEM_DISPLAY, 0, Vec3(0.0, 0.0, 0.0), 0.0
        ))

        val dataValues = entityData.nonDefaultValues ?: mutableListOf()
        refreshData.forEach { item ->
            if (dataValues.none { it.id == item.accessor.id }) {
                dataValues.add(item.value())
            }
        }
        sendPacket(playerId, createDataPacketRaw(playerId, distanceSquared, dataValues, false) ?: return)
    }

    override fun addOrRefreshViewer(playerId: UUID, distanceSquared: Double) {
        if (this.viewers.add(playerId)) {
            addViewer(playerId, distanceSquared)
        } else {
            refreshViewer(playerId, distanceSquared)
        }
    }

    override fun refreshViewer(playerId: UUID, distanceSquared: Double) {
        if (refreshesFrozen) return
        val packet = createDataPacket(playerId, distanceSquared, refreshData, true) ?: return
        sendPacket(playerId, packet)
    }

    override fun hasViewer(playerId: UUID) = playerId in this.viewers

    override fun removeViewer(playerId: UUID): Boolean {
        if (this.viewers.remove(playerId)) {
            sendPacket(playerId, ClientboundRemoveEntitiesPacket(id))
            return true
        }
        return false
    }

    override fun removeAllViewers() {
        for (viewer in viewers.toSet()) {
            removeViewer(viewer)
        }
    }

    override fun spawn() {
        if (isSpawned) return
        isSpawned = true
        for (playerId in viewers) {
            addViewer(playerId, distanceSquared(this, playerId))
        }
    }

    fun tryUpdateLighting(): Boolean {
        // Require at least 1 tick between lighting update attempts, we cannot just ignore
        // all lighting update attempts within 1 tick because we are dealing with multiple possible
        // light locations, so at the worst it gets scheduled to update the next tick.
        if (lightingUpdateCooldown > System.currentTimeMillis()) {
            lightingUpdateScheduled = true
            return false
        }
        lightingUpdateCooldown = System.currentTimeMillis() + 50
        lightingUpdateScheduled = false

        val originalDelegates = this.lightDelegatePositions
        val originalOffset = this.lightingOffset
        this.lightingOffset = calculateLightingOffset()
        if (this.lightingOffset === originalOffset) {
            return false
        }
        this.centerOffset = calculateCenterOffset()

        // prevent refreshes for 1 second to avoid refreshing while the client is updating the position & transformation
        this.refreshesFrozen = true
        this.refreshesFrozenExpireTime = System.currentTimeMillis() + 1000L

        val positionPacket = ClientboundEntityPositionSyncPacket(id, PositionMoveRotation(this.position, ZERO_VEC, 0.0F, 0.0F), false)
        for (viewer in viewers.toSet()) {
            sendPacket(viewer, ClientboundBundlePacket(mutableListOf<Packet<in ClientGamePacketListener>>(
                positionPacket,
                createDataPacket(viewer, distanceSquared(this, viewer), lightingData, false, true)!!
            )))

            val job = BlockCullingEngine.getCullingJob(viewer) ?: continue
            originalDelegates.forEach {
                val delegates = job.lightDelegates[it] ?: return@forEach
                delegates.remove(block)
                if (delegates.isEmpty()) job.lightDelegates.remove(it)
            }

            lightDelegatePositions.forEach {
                job.lightDelegates.getOrPut(it) { CopyOnWriteArraySet() }.add(block)
            }
        }

        return true
    }

    fun tryUpdateState(): Boolean {
        if (!stateUpdated) {
            val newState = (block.block as CraftBlock).blockState
            if (newState === lastState) return false
            lastState = newState

            block.refreshBlockTextureItem()
            stateUpdated = tryUpdateLighting() || itemUpdateData.isDirty
            if (stateUpdated) {
                centerOffset = calculateCenterOffset()
            }
        }
        return stateUpdated
    }

    private fun calculateLightingOffset(): Vector3f {
        if (directLighting) {
            return ZERO_VECTOR
        }

        // Check for the direct neighboring block that has lighting data with the highest light
        var brightestFace: BlockFace? = null
        var brightest = lightingOffset
        var brightestLight = 0.toByte()
        if (brightest !== ZERO_VECTOR) {
            val delegateBlock = block.block.getRelative(brightest.x.toInt(), brightest.y.toInt(), brightest.z.toInt()) as? CraftBlock
            if (delegateBlock?.blockState?.hasLightingData ?: false) {
                brightestLight = delegateBlock.lightLevel
                if (brightestLight >= 15) return brightest
                brightestFace = IMMEDIATE_FACES.firstOrNull { it.modX == brightest.x.toInt() && it.modY == brightest.y.toInt() && it.modZ == brightest.z.toInt() }
            }
        }

        for (face in IMMEDIATE_FACES) {
            if (brightestFace == face) continue
            val delegateBlock = block.block.getRelative(face) as? CraftBlock ?: continue
            if (delegateBlock.blockState.hasLightingData) {
                val light = delegateBlock.lightLevel
                if (light > brightestLight) {
                    brightestLight = light
                    brightest = Vector3f(face.modX.toFloat(), face.modY.toFloat(), face.modZ.toFloat())
                    if (light >= 15) return brightest
                }
            }
        }
        // If none of them do then the block won't be visible anyway.
        return brightest
    }

    private fun calculateCenterOffset(): Vector3f {
        val block = (this.block.block as CraftBlock)
        val shape = this.lastState.getShape(block.level, block.position)
        if (shape.isEmpty) return ZERO_VECTOR

        val center = shape.bounds().center
        return Vector3f(
            (center.x - 0.5).toFloat(),
            (center.y - 0.5).toFloat(),
            (center.z - 0.5).toFloat()
        )
    }

    companion object {

        private val ZERO_VEC = Vec3(0.0, 0.0, 0.0)
        private val ZERO_VECTOR = Vector3f()
        private val ZERO_QUATERNION = Quaternionf()

        private val REFRESH_IDS = listOf(
            EntityDataAccess.DISPLAY_DATA_SCALE_ID,
            EntityDataAccess.DISPLAY_DATA_TRANSLATION_ID
        )

        private val INTERPOLATION_IDS = listOf(
            EntityDataAccess.DISPLAY_DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID,
            EntityDataAccess.DISPLAY_DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID
        )

        @JvmSynthetic
        internal val tickJob = Rebar.scope.launch {
            while (true) {
                // TODO: Consider making this configurable & async, the delay is the delay between making a change to the entity & it being reflected to the clients
                delayTicks(1)
                for (blockTextureOctree in BlockCullingEngine.blockTextureOctrees.values) {
                    for (block in blockTextureOctree) {
                        val entity = block.blockTextureEntity as? BlockTextureEntityImpl ?: continue
                        if (entity.viewers.isEmpty()) continue

                        if (entity.stateUpdated) {
                            entity.stateUpdated = false
                        }

                        if (entity.lightingUpdateScheduled) {
                            entity.tryUpdateLighting()
                        }

                        if (entity.refreshesFrozen && entity.refreshesFrozenExpireTime < System.currentTimeMillis()) {
                            entity.refreshesFrozen = false
                        }

                        val trackedData = entity.entityData.packDirty() ?: continue
                        for (playerId in entity.viewers) {
                            entity.sendPacket(playerId, entity.createDataPacketRaw(playerId, distanceSquared(entity, playerId), trackedData, false, false) ?: continue)
                        }
                    }
                }
            }
        }

        private val BlockState.hasLightingData: Boolean
            get() = !(canOcclude() && `moonrise$occludesFullBlock`())

        private fun distanceSquared(entity: BlockTextureEntityImpl, playerId: UUID): Double {
            val player = Bukkit.getPlayer(playerId) ?: return 0.0
            val block = entity.block.block
            val dx = player.x - block.x
            val dy = player.y - block.y
            val dz = player.z - block.z
            return dx * dx + dy * dy + dz * dz
        }
    }
}