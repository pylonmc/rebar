package io.github.pylonmc.rebar.nms.entity

import com.mojang.math.MatrixUtil
import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.culling.BlockCullingEngine
import io.github.pylonmc.rebar.entity.display.transform.TransformUtil.toMatrix
import io.github.pylonmc.rebar.entity.packet.BlockTextureEntity
import io.github.pylonmc.rebar.entity.packet.BlockTextureEntity.Companion.MAX_SCALE_INCREASE
import io.github.pylonmc.rebar.entity.packet.BlockTextureEntity.Companion.SCALE_DISTANCE_INCREASE
import io.github.pylonmc.rebar.entity.packet.BlockTextureEntity.Companion.SCALE_DISTANCE_THRESHOLD
import io.github.pylonmc.rebar.util.delayTicks
import kotlinx.coroutines.launch
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SyncedDataHolder
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Pose
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.phys.Vec3
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Display
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Transformation
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.Vector3fc
import java.util.Optional
import java.util.UUID
import kotlin.math.abs
import kotlin.math.min

import net.minecraft.util.Brightness as NmsBrightness
import net.minecraft.world.entity.Display as NmsDisplay
import net.minecraft.world.item.ItemStack as NmsItemStack

class BlockTextureEntityImpl : BlockTextureEntity, SyncedDataHolder {
    override val block: RebarBlock
    override var id: Int
    override var uuid: UUID
    override val viewers: MutableSet<UUID> = mutableSetOf()

    override var isSpawned: Boolean = false

    private val entityData: SynchedEntityData

    /**
     * The data to sync when the entity is spawned in
     */
    private val trackedDataValues: List<SynchedEntityData.DataValue<*>>

    /**
     * The data to sync on player refresh
     */
    private val refreshDataValues: List<SynchedEntityData.DataValue<*>>

    private val lastScaleIncreases: MutableMap<UUID, Float> = mutableMapOf()

    /**
     * The transformation of the texture entity needs to be centered on the bounding box of the block instead of the block center.
     * This is needed so that the scaling of non-full blocks (like slabs) scales around the correct point to combat z-fighting.
     * The bounding box is unlikely to change often, so we cache the translation and only recalculate it every 5 seconds when requested.
     */
    private var centerTranslation = Vector3f()
        get() {
            if (System.currentTimeMillis() - translationTimestamp > 5000) {
                field = calculateCenterTranslation()
                translationTimestamp = System.currentTimeMillis()
            }
            return field
        }
    private var translationTimestamp = 0L

    constructor(block: RebarBlock) {
        this.block = block
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
            define(EntityDataAccess.DISPLAY_DATA_TRANSLATION_ID, Vector3f())
            define(EntityDataAccess.DISPLAY_DATA_SCALE_ID, Vector3f(1.0F, 1.0F, 1.0F))
            define(EntityDataAccess.DISPLAY_DATA_RIGHT_ROTATION_ID, Quaternionf())
            define(EntityDataAccess.DISPLAY_DATA_LEFT_ROTATION_ID, Quaternionf())
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
        this.trackedDataValues = entityData.nonDefaultValues ?: listOf()
        this.refreshDataValues = trackedDataValues.filter { it.id == EntityDataAccess.DISPLAY_DATA_SCALE_ID.id || it.id == EntityDataAccess.DISPLAY_DATA_TRANSLATION_ID.id}
    }

    override var transformation: Transformation
        get() = Transformation(
            Vector3f(entityData.get(EntityDataAccess.DISPLAY_DATA_TRANSLATION_ID)),
            Quaternionf(entityData.get(EntityDataAccess.DISPLAY_DATA_LEFT_ROTATION_ID)),
            Vector3f(entityData.get(EntityDataAccess.DISPLAY_DATA_SCALE_ID)),
            Quaternionf(entityData.get(EntityDataAccess.DISPLAY_DATA_RIGHT_ROTATION_ID))
        )
        set(value) {
            entityData.set(EntityDataAccess.DISPLAY_DATA_TRANSLATION_ID, value.translation)
            entityData.set(EntityDataAccess.DISPLAY_DATA_LEFT_ROTATION_ID, value.leftRotation)
            entityData.set(EntityDataAccess.DISPLAY_DATA_SCALE_ID, value.scale)
            entityData.set(EntityDataAccess.DISPLAY_DATA_RIGHT_ROTATION_ID, value.rightRotation)
        }
    override var transformationMatrix: Matrix4f
        get() = transformation.toMatrix()
        set(value) {
            val f = 1.0F / value.m33();
            val decomposed = MatrixUtil.svdDecompose(Matrix3f(value).scale(f))
            entityData.set(EntityDataAccess.DISPLAY_DATA_TRANSLATION_ID, value.getTranslation(Vector3f()).mul(f))
            entityData.set(EntityDataAccess.DISPLAY_DATA_LEFT_ROTATION_ID, Quaternionf(decomposed.left))
            entityData.set(EntityDataAccess.DISPLAY_DATA_SCALE_ID, Vector3f(decomposed.middle))
            entityData.set(EntityDataAccess.DISPLAY_DATA_RIGHT_ROTATION_ID, Quaternionf(decomposed.right))
        }

    override var interpolationDelay: Int
        get() = entityData.get(EntityDataAccess.DISPLAY_DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID)
        set(value) = entityData.set(EntityDataAccess.DISPLAY_DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID, value)
    override var interpolationDuration: Int
        get() = entityData.get(EntityDataAccess.DISPLAY_DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID)
        set(value) = entityData.set(EntityDataAccess.DISPLAY_DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID, value)
    override var teleportDuration: Int
        get() = entityData.get(EntityDataAccess.DISPLAY_DATA_POS_ROT_INTERPOLATION_DURATION_ID)
        set(value) = entityData.set(EntityDataAccess.DISPLAY_DATA_POS_ROT_INTERPOLATION_DURATION_ID, value)

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
            else -> NmsBrightness.unpack(brightness).let { Display.Brightness(it.block, it.sky) }
        }
        set(value) = entityData.set(EntityDataAccess.DISPLAY_DATA_BRIGHTNESS_OVERRIDE_ID, value?.let { NmsBrightness(value.blockLight, value.skyLight).pack() } ?: -1)

    override var itemStack: ItemStack?
        get() = entityData.get(EntityDataAccess.ITEM_DISPLAY_DATA_ITEM_STACK_ID).asBukkitCopy()
        set(value) = entityData.set(EntityDataAccess.ITEM_DISPLAY_DATA_ITEM_STACK_ID, (value as? CraftItemStack)?.handle ?: NmsItemStack.EMPTY)
    override var itemDisplayTransform: ItemDisplay.ItemDisplayTransform
        get() = ItemDisplay.ItemDisplayTransform.entries[entityData.get(EntityDataAccess.ITEM_DISPLAY_DATA_ITEM_DISPLAY_ID).toInt()]
        set(value) = entityData.set(EntityDataAccess.ITEM_DISPLAY_DATA_ITEM_DISPLAY_ID, value.ordinal.toByte())

    private fun calculateCenterTranslation(): Vector3f {
        val boundingBox = block.block.boundingBox
        val blockX = block.block.x
        val blockY = block.block.y
        val blockZ = block.block.z

        val bbCenterX = (boundingBox.minX + boundingBox.maxX) / 2.0 - blockX
        val bbCenterY = (boundingBox.minY + boundingBox.maxY) / 2.0 - blockY
        val bbCenterZ = (boundingBox.minZ + boundingBox.maxZ) / 2.0 - blockZ

        return Vector3f(
            (bbCenterX - 0.5).toFloat(),
            (bbCenterY - 0.5).toFloat(),
            (bbCenterZ - 0.5).toFloat()
        )
    }

    override fun onSyncedDataUpdated(p0: EntityDataAccessor<*>) {}
    override fun onSyncedDataUpdated(p0: List<SynchedEntityData.DataValue<*>>) {}

    private fun createDataPacket(playerId: UUID, distanceSquared: Double, trackedData: List<SynchedEntityData.DataValue<*>>, refresh: Boolean) : ClientboundSetEntityDataPacket? {
        var trackedData = trackedData
        val scaleIncrease = min((distanceSquared * SCALE_DISTANCE_INCREASE).toFloat(), MAX_SCALE_INCREASE)
        val lastScaleIncrease = lastScaleIncreases[playerId] ?: 0f
        if (abs(scaleIncrease - lastScaleIncrease) < SCALE_DISTANCE_THRESHOLD && refresh) {
            return null
        }

        lastScaleIncreases[playerId] = scaleIncrease
        var scale: Vector3f? = null
        trackedData = trackedData.map { data ->
            if (data.id == EntityDataAccess.DISPLAY_DATA_SCALE_ID.id) {
                scale = Vector3f(scaleIncrease).add(data.value as Vector3fc)
                SynchedEntityData.DataValue(data.id, EntityDataSerializers.VECTOR3, scale)
            } else {
                data
            }
        }

        return ClientboundSetEntityDataPacket(id, trackedData.map { data ->
            if (data.id == EntityDataAccess.DISPLAY_DATA_TRANSLATION_ID.id && scale != null) {
                SynchedEntityData.DataValue(data.id, EntityDataSerializers.VECTOR3, Vector3f(
                    centerTranslation.x * (1 - scale.x),
                    centerTranslation.y * (1 - scale.y),
                    centerTranslation.z * (1 - scale.z)
                ))
            } else {
                data
            }
        })
    }

    private fun sendPacket(playerId: UUID, packet: Packet<*>) {
        val player = (Bukkit.getPlayer(playerId) as CraftPlayer).handle
        player.connection.send(packet)
    }

    private fun addViewer(playerId: UUID, distanceSquared: Double) {
        sendPacket(playerId, ClientboundAddEntityPacket(
            id, uuid, block.block.x + 0.5, block.block.y + 0.5, block.block.z + 0.5, 0f, 0f,
            EntityType.ITEM_DISPLAY, 0, Vec3(0.0, 0.0, 0.0), 0.0
        ))
        sendPacket(playerId, createDataPacket(playerId, distanceSquared, trackedDataValues, false) ?: return)
    }

    override fun addOrRefreshViewer(playerId: UUID, distanceSquared: Double) {
        if (this.viewers.add(playerId)) {
            addViewer(playerId, distanceSquared)
        } else {
            refreshViewer(playerId, distanceSquared)
        }
    }

    override fun refreshViewer(playerId: UUID, distanceSquared: Double) {
        sendPacket(playerId, createDataPacket(playerId, distanceSquared, refreshDataValues, true) ?: return)
    }

    override fun hasViewer(playerId: UUID) = playerId in this.viewers

    override fun removeViewer(playerId: UUID) {
        if (this.viewers.remove(playerId)) {
            sendPacket(playerId, ClientboundRemoveEntitiesPacket(id))
        }
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

    companion object {

        @JvmSynthetic
        internal val tickJob = Rebar.scope.launch {
            while (true) {
                // TODO: Consider making this configurable & async, the delay is the delay between making a change to the entity & it being reflected to the clients
                delayTicks(1)
                for (blockTextureOctree in BlockCullingEngine.blockTextureOctrees.values) {
                    for (block in blockTextureOctree) {
                        val entity = block.blockTextureEntity as? BlockTextureEntityImpl ?: continue
                        if (entity.viewers.isEmpty()) continue

                        val trackedData = entity.entityData.packDirty() ?: continue
                        for (playerId in entity.viewers) {
                            entity.sendPacket(playerId, entity.createDataPacket(playerId, distanceSquared(entity, playerId), trackedData, false) ?: continue)
                        }
                    }
                }
            }
        }

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