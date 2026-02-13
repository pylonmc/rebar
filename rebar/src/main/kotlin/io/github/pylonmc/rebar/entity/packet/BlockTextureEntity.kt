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
        val scale = entityMeta.metadata.getIndex(SCALE_INDEX.toByte(), null as Vector3f?) ?: return
        val metadata = arrayListOf(EntityData(SCALE_INDEX, EntityDataTypes.VECTOR3F, Vector3f(scale.x, scale.y, scale.z))) as List<EntityData<*>>
        sendPacketToViewer(viewer, WrapperPlayServerEntityMetadata(entityId, metadata), distanceSquared)
    }

    open fun sendPacketToViewer(viewer: UUID, wrapper: PacketWrapper<*>, distanceSquared: Double) {
        var packet = wrapper
        if (packet is WrapperPlayServerEntityMetadata) {
            val scaleIncrease = (min(distanceSquared, 1600.0) * SCALE_DISTANCE_INCREASE / 20.0).toFloat()
            val lastScaleIncrease = lastScaleIncreases[viewer] ?: 0f
            if (abs(scaleIncrease - lastScaleIncrease) < 0.00001f && packet.entityMetadata.size == 1) {
                return
            }
            lastScaleIncreases[viewer] = scaleIncrease

            val metadata = ArrayList(packet.entityMetadata)
            var scale = metadata.find { it.index == SCALE_INDEX && it.type == EntityDataTypes.VECTOR3F } ?: return
            val index = metadata.indexOf(scale)
            scale = EntityData(SCALE_INDEX, EntityDataTypes.VECTOR3F, (scale.value as Vector3f).add(scaleIncrease, scaleIncrease, scaleIncrease))
            metadata[index] = scale

            if (metadata.size > 1) {
                val playerTranslator = NmsAccessor.instance.getTranslationHandler(viewer)
                if (playerTranslator != null) {
                    for (i in metadata.indices) {
                        val value = metadata[i].value
                        if (value is ItemStack) {
                            val bukkitStack = SpigotConversionUtil.toBukkitItemStack(value)
                            playerTranslator.handleItem(bukkitStack)
                            metadata[i] = EntityData<ItemStack>(
                                metadata[i].index,
                                EntityDataTypes.ITEMSTACK,
                                SpigotConversionUtil.fromBukkitItemStack(bukkitStack)
                            )
                        } else if (value is Optional<*> && value.isPresent && value.get() is ItemStack) {
                            val bukkitStack = SpigotConversionUtil.toBukkitItemStack(value.get() as ItemStack)
                            playerTranslator.handleItem(bukkitStack)
                            metadata[i] = EntityData<Optional<ItemStack>>(
                                metadata[i].index,
                                EntityDataTypes.OPTIONAL_ITEMSTACK,
                                Optional.of(SpigotConversionUtil.fromBukkitItemStack(bukkitStack))
                            )
                        }
                    }
                }
            }

            packet = WrapperPlayServerEntityMetadata(packet.entityId, metadata)
        }

        val protocolManager = PacketEvents.getAPI().protocolManager ?: return
        val channel = protocolManager.getChannel(viewer) ?: return
        protocolManager.sendPacket(channel, packet)
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

    companion object {
        const val SCALE_INDEX = 12
        const val SCALE_DISTANCE_INCREASE = 0.0015f
        const val BLOCK_OVERLAP_SCALE = 1.0005f
    }
}