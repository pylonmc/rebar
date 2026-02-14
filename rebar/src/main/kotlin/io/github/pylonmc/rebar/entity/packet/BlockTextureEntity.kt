package io.github.pylonmc.rebar.entity.packet

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.PacketEventsAPI
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.util.Vector3f
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.nms.NmsAccessor
import io.github.pylonmc.rebar.resourcepack.block.BlockTextureEngine
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import me.tofaa.entitylib.EntityLib
import me.tofaa.entitylib.wrapper.WrapperEntity
import java.util.*
import kotlin.math.abs
import kotlin.math.min

/**
 * A specific [WrapperEntity] for [RebarBlock] textures. It is an item display entity
 * that will scale based on the distance to the viewer to prevent z-fighting with the
 * block overlaid upon.
 * 
 * (see [RebarBlock.blockTextureEntity] and [BlockTextureEngine])
 */
open class BlockTextureEntity(
    val block: RebarBlock
) : WrapperEntity(EntityTypes.ITEM_DISPLAY) {

    private val lastScaleIncreases = mutableMapOf<UUID, Float>()

    /**
     * The translation needed to center scaling on the bounding box center instead of the block center.
     * This is needed so that the scaling of non-full blocks (like slabs) scales around the correct point.
     * The bounding box is unlikely to change often, so we cache the translation and only recalculate it every 5 seconds when requested.
     */
    private var scaleTranslation = calculateScaleTranslation()
        get() {
            if (System.currentTimeMillis() - translationTimestamp > 5000) {
                field = calculateScaleTranslation()
                translationTimestamp = System.currentTimeMillis()
            }
            return field
        }
    private var translationTimestamp = System.currentTimeMillis()

    open fun addOrRefreshViewer(viewer: UUID, distanceSquared: Double) {
        if (this.viewers.add(viewer)) {
            if (location != null && isSpawned) {
                sendPacketToViewer(viewer, this.createSpawnPacket(), distanceSquared)
                sendPacketToViewer(viewer, this.entityMeta.createPacket(), distanceSquared)
            }
        } else if (location != null && isSpawned) {
            refreshViewer(viewer, distanceSquared)
        }
    }

    open fun refreshViewer(viewer: UUID, distanceSquared: Double) {
        val scale = entityMeta.metadata.getIndex(SCALE_INDEX.toByte(), null as Vector3f?) ?: DEFAULT_SCALE
        val translation = entityMeta.metadata.getIndex(TRANSLATION_INDEX.toByte(), null as Vector3f?) ?: DEFAULT_TRANSLATION
        val metadata = arrayListOf(
            EntityData(TRANSLATION_INDEX, EntityDataTypes.VECTOR3F, translation),
            EntityData(SCALE_INDEX, EntityDataTypes.VECTOR3F, scale)
        ) as List<EntityData<*>>
        sendPacketToViewer(viewer, WrapperPlayServerEntityMetadata(entityId, metadata), distanceSquared, true)
    }

    open fun sendPacketToViewer(viewer: UUID, wrapper: PacketWrapper<*>, distanceSquared: Double, refresh: Boolean = false) {
        if (wrapper is WrapperPlayServerEntityMetadata) {
            if (!refresh) {
                val playerTranslator = NmsAccessor.instance.getTranslationHandler(viewer)
                if (playerTranslator != null) {
                    wrapper.entityMetadata = ArrayList(wrapper.entityMetadata)
                    for (i in wrapper.entityMetadata.indices) {
                        val value = wrapper.entityMetadata[i].value
                        if (value is ItemStack) {
                            val bukkitStack = SpigotConversionUtil.toBukkitItemStack(value)
                            playerTranslator.handleItem(bukkitStack)
                            wrapper.entityMetadata[i] = EntityData<ItemStack>(
                                wrapper.entityMetadata[i].index,
                                EntityDataTypes.ITEMSTACK,
                                SpigotConversionUtil.fromBukkitItemStack(bukkitStack)
                            )
                        } else if (value is Optional<*> && value.isPresent && value.get() is ItemStack) {
                            val bukkitStack = SpigotConversionUtil.toBukkitItemStack(value.get() as ItemStack)
                            playerTranslator.handleItem(bukkitStack)
                            wrapper.entityMetadata[i] = EntityData<Optional<ItemStack>>(
                                wrapper.entityMetadata[i].index,
                                EntityDataTypes.OPTIONAL_ITEMSTACK,
                                Optional.of(SpigotConversionUtil.fromBukkitItemStack(bukkitStack))
                            )
                        }
                    }
                }
            } else {
                val scaleIncrease = min((distanceSquared * SCALE_DISTANCE_INCREASE).toFloat(), MAX_SCALE_INCREASE)
                val lastScaleIncrease = lastScaleIncreases[viewer] ?: 0f
                if (abs(scaleIncrease - lastScaleIncrease) < SCALE_DISTANCE_THRESHOLD && wrapper.entityMetadata.size == 2) {
                    return
                }

                lastScaleIncreases[viewer] = scaleIncrease

                @Suppress("UNCHECKED_CAST")
                val scaleData = wrapper.entityMetadata.find { it.index == SCALE_INDEX && it.type == EntityDataTypes.VECTOR3F } as? EntityData<Vector3f> ?: return
                val scale = (scaleData.value as Vector3f).add(scaleIncrease, scaleIncrease, scaleIncrease)
                scaleData.value = scale

                @Suppress("UNCHECKED_CAST")
                val translationData = wrapper.entityMetadata.find { it.index == TRANSLATION_INDEX && it.type == EntityDataTypes.VECTOR3F } as? EntityData<Vector3f> ?: return
                val translation = Vector3f(scaleTranslation.x * (1 - scale.x), scaleTranslation.y * (1 - scale.y), scaleTranslation.z * (1 - scale.z))
                translationData.value = translation
            }
        }

        val protocolManager = PacketEvents.getAPI().protocolManager ?: return
        val channel = protocolManager.getChannel(viewer) ?: return
        protocolManager.sendPacket(channel, wrapper)
    }

    override fun removeViewer(uuid: UUID) {
        super.removeViewer(uuid)
        lastScaleIncreases.remove(uuid)
    }

    open fun removeAllViewers() {
        for (viewer in viewers.toSet()) {
            removeViewer(viewer)
        }
    }

    private fun calculateScaleTranslation(): Vector3f {
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

    companion object {
        const val SCALE_INDEX = 12
        const val TRANSLATION_INDEX = 11

        val DEFAULT_SCALE = Vector3f(1f, 1f, 1f)
        val DEFAULT_TRANSLATION = Vector3f(0f, 0f, 0f)

        /**
         * A base scale to prevent z-fighting between the block and item display when the player is right next to a block.
         */
        const val BLOCK_OVERLAP_INCREASE = 0.0005f

        /**
         * The maximum scale increase to prevent the item display from becoming too large at long distances.
         */
        const val MAX_SCALE_INCREASE = 0.2f

        /**
         * The problem with making the item display too large is that the block break overlay will be obscured by the item display.
         * This issue is mainly only prevalent when close to the block, so the farther away they are from the block the more we can
         * increase the scale to prevent z-fighting without worrying about the block break overlay being obscured.
         */
        const val DOUBLE_OVERLAP_INCREASE_DISTANCE = 2

        /**
         * Calculate scale increase so that at EXPECTED_REACH_DISTANCE, the scale increase is double BLOCK_OVERLAP_SCALE
         */
        const val SCALE_DISTANCE_INCREASE = BLOCK_OVERLAP_INCREASE / (DOUBLE_OVERLAP_INCREASE_DISTANCE * DOUBLE_OVERLAP_INCREASE_DISTANCE)

        /**
         * If the difference between the new scale increase and the last scale increase sent to the viewer is less than 1/10th of the base BLOCK_OVERLAP_SCALE, we don't send a packet to avoid unnecessary packet spam.
         */
        const val SCALE_DISTANCE_THRESHOLD = BLOCK_OVERLAP_INCREASE / 10.0f
    }
}