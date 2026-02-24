package io.github.pylonmc.rebar.block.base

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.github.shynixn.mccoroutine.bukkit.ticks
import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.block.base.RebarCargoBlock.Companion.cargoItemsTransferredPerSecond
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.content.cargo.CargoDuct
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.entity.display.ItemDisplayBuilder
import io.github.pylonmc.rebar.entity.display.transform.LineBuilder
import io.github.pylonmc.rebar.event.*
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.logistics.CargoRoutes
import io.github.pylonmc.rebar.logistics.LogisticGroup
import io.github.pylonmc.rebar.logistics.LogisticGroupType
import io.github.pylonmc.rebar.util.IMMEDIATE_FACES
import io.github.pylonmc.rebar.util.rebarKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.jetbrains.annotations.ApiStatus
import org.joml.Vector3d
import java.util.IdentityHashMap
import kotlin.math.min

/**
 * Represents a block that can connect to cargo ducts and use them to interface
 * with other cargo RebarCargoBlocks.
 *
 * Each face can have one logistic group which cargo ducts connected to that face
 * are allowed to interface with
 *
 * In your place constructor, you will need to call [addCargoLogisticGroup] for all
 * the block faces you want to be able to connect cargo ducts to, and also
 * `setCargoTransferRate` to set the maximum number of items that can be transferred
 * out of this block per cargo tick.
 */
interface RebarCargoBlock : RebarLogisticBlock, RebarEntityHolderBlock {

    private val cargoBlockData: CargoBlockData
        get() = cargoBlocks.getOrPut(this) { CargoBlockData(
            mutableMapOf(),
            1
        )}

    @ApiStatus.NonExtendable
    fun addCargoLogisticGroup(face: BlockFace, group: String) {
        cargoBlockData.groups.put(face, group)
    }

    @ApiStatus.NonExtendable
    fun removeCargoLogisticGroup(face: BlockFace) {
        cargoBlockData.groups.remove(face)
    }

    @ApiStatus.NonExtendable
    fun getCargoLogisticGroup(face: BlockFace): LogisticGroup?
            = cargoBlockData.groups[face]?.let { getLogisticGroup(it) }

    val cargoLogisticGroups: Map<BlockFace, String>
        @ApiStatus.NonExtendable
        get() = cargoBlockData.groups.toMap()

    var cargoTransferRate: Int
        /**
         * Note that [cargoTransferRate] will be multiplied by [RebarConfig.CARGO_TRANSFER_RATE_MULTIPLIER],
         * and the result will be the maximum number of items that can be transferred
         * out of this block per cargo tick.
         *
         * @see [cargoItemsTransferredPerSecond]
         */
        @ApiStatus.NonExtendable
        set(transferRate) {
            cargoBlockData.transferRate = transferRate
        }
        @ApiStatus.NonExtendable
        get() = cargoBlockData.transferRate

    fun onDuctConnected(event: RebarCargoConnectEvent, priority: EventPriority) {}

    fun onDuctDisconnected(event: RebarCargoDisconnectEvent, priority: EventPriority) {}

    /**
     * Checks if the block can connect to any adjacent cargo blocks, and if so, creates
     * a duct display between this block and the adjacent cargo block in question.
     */
    @ApiStatus.NonExtendable
    fun updateDirectlyConnectedFaces() {
        for (face in IMMEDIATE_FACES) {
            // We iterate IMMEDIATE_FACES instead of [cargoBlockData.groups] in case [cargoBlockData.groups] is
            // modified at some point while iterating, e.g. by a RebarCargoConnectEvent listener
            if (face !in cargoBlockData.groups || getHeldEntity("cargo:direct-connection:${face}") != null) {
                continue
            }

            val otherBlock = BlockStorage.get(block.getRelative(face))
            if (otherBlock !is RebarCargoBlock
                || face.oppositeFace !in otherBlock.cargoBlockData.groups
                || !RebarCargoConnectEvent(this as RebarBlock, otherBlock).callEvent()
            ) {
                continue
            }

            val fromLocation = block.location.toCenterLocation()
            val toLocation = otherBlock.block.location.toCenterLocation()
            val display = ItemDisplayBuilder()
                .transformation(
                    LineBuilder()
                        .from(Vector3d())
                        .to(toLocation.subtract(fromLocation).toVector().toVector3d())
                        .thickness(0.3505)
                        .extraLength(0.3505)
                        .build()
                )
                .itemStack(ItemStackBuilder.of(Material.GRAY_CONCRETE)
                    .addCustomModelDataString("pylon:cargo_duct:line"))
                .build(fromLocation)
            addEntity("cargo:direct-connection:${face}", display)
        }
    }

    fun tickCargo() {
        for ((face, group) in cargoBlockData.groups) {
            val sourceGroup = getLogisticGroup(group)
            if (sourceGroup == null || sourceGroup.slotType == LogisticGroupType.INPUT) {
                continue
            }

            val target = CargoRoutes.getCargoTarget(this, face)
            if (target == null || target.block.block == block) {
                continue
            }

            val targetGroup = target.block.getCargoLogisticGroup(target.face)
            if (targetGroup == null || targetGroup.slotType == LogisticGroupType.OUTPUT) {
                continue
            }


            tickCargoFace(sourceGroup, targetGroup)
        }
    }

    fun tickCargoFace(sourceGroup: LogisticGroup, targetGroup: LogisticGroup) {
        for (sourceSlot in sourceGroup.slots) {
            val sourceStack = sourceSlot.getItemStack()
            if (sourceStack == null || (targetGroup.filter != null && !targetGroup.filter!!(sourceStack))) {
                continue
            }

            var wasTargetModified = false
            var remainingAvailableTransfers = cargoBlockData.transferRate.toLong() * RebarConfig.CARGO_TRANSFER_RATE_MULTIPLIER
            for (targetSlot in targetGroup.slots) {
                val sourceAmount = sourceSlot.getAmount()
                val targetStack = targetSlot.getItemStack()
                val targetAmount = targetSlot.getAmount()
                val targetMaxAmount = targetSlot.getMaxAmount(sourceStack)

                if (targetAmount == targetMaxAmount || (targetStack != null && !targetStack.isEmpty && !sourceStack.isSimilar(targetStack))) {
                    continue
                }

                val toTransfer = min(
                    remainingAvailableTransfers,
                    min(targetMaxAmount - targetAmount, sourceAmount),
                )

                if (sourceAmount == toTransfer) {
                    sourceSlot.set(null, 0)
                } else {
                    sourceSlot.set(sourceStack, sourceAmount - toTransfer)
                }
                targetSlot.set(sourceStack, targetAmount + toTransfer)

                remainingAvailableTransfers -= toTransfer
                if (remainingAvailableTransfers <= 0) {
                    return
                }

                wasTargetModified = true
            }

            if (wasTargetModified) {
                // We've already partially transferred one source slot; don't try to transfer any others
                return
            }
        }
    }

    @ApiStatus.Internal
    companion object : MultiListener {

        @JvmStatic
        fun cargoItemsTransferredPerSecond(cargoTransferRate: Int)
            = (20 * cargoTransferRate * RebarConfig.CARGO_TRANSFER_RATE_MULTIPLIER).toDouble() / RebarConfig.CARGO_TICK_INTERVAL.toDouble()

        internal data class CargoBlockData(
            var groups: MutableMap<BlockFace, String>,
            var transferRate: Int,
        )

        private val cargoBlockKey = rebarKey("cargo_block_data")

        private val cargoBlocks = IdentityHashMap<RebarCargoBlock, CargoBlockData>()
        private val cargoTickers = IdentityHashMap<RebarCargoBlock, Job>()

        private fun startTicker(block: RebarCargoBlock) {
            cargoTickers[block] = Rebar.launch(Rebar.minecraftDispatcher) {
                while (true) {
                    delay(RebarConfig.CARGO_TICK_INTERVAL.ticks)
                    block.tickCargo()
                }
            }
        }

        @EventHandler
        private fun onBreak(event: RebarBlockBreakEvent) {
            val block = event.rebarBlock
            if (block !is RebarCargoBlock) {
                return
            }

            // Disconnect from directly adjacent cargo blocks
            for (face in block.cargoBlockData.groups.toMap().keys) {
                val otherBlock = BlockStorage.get(block.block.getRelative(face))
                if (otherBlock is RebarCargoBlock && face.oppositeFace in otherBlock.cargoBlockData.groups) {
                    otherBlock.getHeldEntity("cargo:direct-connection:${face.oppositeFace}")?.remove()
                    RebarCargoDisconnectEvent(otherBlock, block).callEvent()
                    otherBlock.updateDirectlyConnectedFaces()
                }
            }

            // Disconnect adjacent cargo ducts
            for ((face, _) in block.cargoBlockData.groups) {
                BlockStorage.getAs<CargoDuct>(block.block.getRelative(face))?.let { duct ->
                    if (face.oppositeFace in duct.connectedFaces) {
                        duct.connectedFaces.remove(face.oppositeFace)
                        duct.updateConnectedFaces()
                        RebarCargoDisconnectEvent(duct, block).callEvent()
                    }
                }
            }

            cargoBlocks.remove(block)
            cargoTickers.remove(block)?.cancel()
        }

        // Should fire after logistic groups have been set up
        @EventHandler(priority = EventPriority.HIGH)
        private fun onPlace(event: RebarBlockPlaceEvent) {
            val block = event.rebarBlock
            if (block !is RebarCargoBlock) {
                return
            }

            // Connect to directly adjacent cargo blocks
            for (face in block.cargoBlockData.groups.toMap().keys) {
                BlockStorage.getAs<RebarCargoBlock>(block.block.getRelative(face))?.updateDirectlyConnectedFaces()
            }

            // Connect adjacent cargo ducts
            for ((face, _) in block.cargoBlockData.groups.toMap()) {
                BlockStorage.getAs<CargoDuct>(block.block.getRelative(face))?.updateConnectedFaces()
            }

            startTicker(block)
        }

        @EventHandler
        private fun onDeserialize(event: RebarBlockDeserializeEvent) {
            val block = event.rebarBlock
            if (block is RebarCargoBlock) {
                cargoBlocks[block] = event.pdc.get(cargoBlockKey, RebarSerializers.CARGO_BLOCK_DATA)
                    ?: error("Ticking block data not found for ${block.key}")
            }
        }

        @EventHandler
        private fun onSerialize(event: RebarBlockSerializeEvent) {
            val block = event.rebarBlock
            if (block is RebarCargoBlock) {
                event.pdc.set(cargoBlockKey, RebarSerializers.CARGO_BLOCK_DATA, cargoBlocks[block]!!)
            }
        }

        @EventHandler
        private fun onLoad(event: RebarBlockLoadEvent) {
            val block = event.rebarBlock
            if (block is RebarCargoBlock) {
                startTicker(block)
            }
        }

        @EventHandler
        private fun onUnload(event: RebarBlockUnloadEvent) {
            val block = event.rebarBlock
            if (block is RebarCargoBlock) {
                cargoBlocks.remove(block)
                cargoTickers.remove(block)?.cancel()
            }
        }

        @UniversalHandler
        private fun onDuctConnected(event: RebarCargoConnectEvent, priority: EventPriority) {
            val block1 = event.block1
            if (block1 is RebarCargoBlock) {
                MultiHandlers.handleEvent(block1, "onDuctConnected", event, priority)
            }
            val block2 = event.block2
            if (block2 is RebarCargoBlock) {
                MultiHandlers.handleEvent(block2, "onDuctConnected", event, priority)
            }
        }

        @UniversalHandler
        private fun onDuctDisconnected(event: RebarCargoDisconnectEvent, priority: EventPriority) {
            val block1 = event.block1
            if (block1 is RebarCargoBlock) {
                MultiHandlers.handleEvent(block1, "onDuctDisconnected", event, priority)
            }
            val block2 = event.block2
            if (block2 is RebarCargoBlock) {
                MultiHandlers.handleEvent(block2, "onDuctDisconnected", event, priority)
            }
        }
    }
}