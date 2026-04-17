package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.event.RebarBlockDeserializeEvent
import io.github.pylonmc.rebar.event.RebarBlockPlaceEvent
import io.github.pylonmc.rebar.event.RebarBlockSerializeEvent
import io.github.pylonmc.rebar.event.RebarBlockUnloadEvent
import io.github.pylonmc.rebar.util.getRelative
import io.github.pylonmc.rebar.util.position.ChunkPosition
import io.github.pylonmc.rebar.util.position.position
import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.util.rotateVectorToFace
import io.github.pylonmc.rebar.waila.Waila
import io.github.pylonmc.rebar.waila.WailaDisplay
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.util.Vector
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.MustBeInvokedByOverriders
import org.joml.Vector3i
import java.util.*
import kotlin.math.abs
import kotlin.math.min

/**
 * A multiblock that is made of a predefined set of components.
 *
 * Automatically creates ghost blocks to show how to build your multiblock.
 *
 * Automatically handles different possible rotations of your multiblock.
 *
 * If you need something more flexible (eg: a fluid tank that can have up to 10
 * fluid casings added to increase the capacity), see [RebarMultiblock].
 */
interface RebarSimpleMultiblock : RebarMultiblock, RebarGhostBlockHolder, RebarEntityCulledBlock {

    /**
     * Represents a single block of a multiblock.
     */
    open class MultiblockComponent protected constructor(
        val vanillaBlocks: List<BlockData>,
        val rebarBlocks: List<NamespacedKey>,
    ) {

        init {
            check(!vanillaBlocks.isEmpty() || !rebarBlocks.isEmpty()) {
                "Multiblock component does not match anything"
            }
        }

        fun matches(block: Block): Boolean {
            val rebarBlock = BlockStorage.get(block)
            if (rebarBlock != null) {
                return rebarBlock.key in rebarBlocks
            }
            return vanillaBlocks.any { block.blockData.matches(it) }
        }

        /**
         * Sets [block] to a 'default' value which matches this MultiblockComponent.
         *
         * For example, if a MultiblockComponent can be grass or dirt, this should set
         * the block to either grass or dirt
         */
        fun placeDefaultBlock(block: Block) {
            if (!block.type.isAir || BlockStorage.isRebarBlock(block)) {
                return
            }

            if (!vanillaBlocks.isEmpty()) {
                block.blockData = vanillaBlocks.first().clone()
            } else {
                BlockStorage.placeBlock(block, rebarBlocks.first())
            }
        }

        /**
         * Spawns the ghost block that visualises this component
         *
         * @see RebarGhostBlockHolder
         */
        fun spawnGhostBlock(multiblock: RebarSimpleMultiblock, rotatedPosition: Vector3i) {
            multiblock.addGhostBlock(rotatedPosition, vanillaBlocks, rebarBlocks)
            updateGhostBlock(multiblock, rotatedPosition)
        }

        /**
         * Updates the corresponding ghost block's visuals
         */
        fun updateGhostBlock(multiblock: RebarSimpleMultiblock, rotatedPosition: Vector3i) {
            val block = multiblock.block.getRelative(rotatedPosition)
            val color = if (matches(block)) {
                Color.LIME
            } else if (block.isEmpty && !BlockStorage.isRebarBlock(block)) {
                Color.ORANGE
            } else {
                Color.RED
            }
            multiblock.getVanillaGhostBlockDisplay(rotatedPosition)?.entity?.glowColorOverride = color
            multiblock.getRebarGhostBlockDisplay(rotatedPosition)?.entity?.glowColorOverride = color
        }

        companion object {

            @JvmStatic
            fun of(blockDatas: List<BlockData>, rebarItems: List<NamespacedKey>) = MultiblockComponent(
                blockDatas, rebarItems
            )

            @JvmStatic
            fun of(vararg materials: Material) = MultiblockComponent(
                materials.map { it.createBlockData() }, listOf()
            )

            @JvmStatic
            fun of(vararg blockDatas: BlockData) = MultiblockComponent(
                blockDatas.toList(), listOf()
            )

            @JvmStatic
            fun of(vararg keys: NamespacedKey) = MultiblockComponent(
                listOf(), keys.toList()
            )
        }
    }

    private val simpleMultiblockData: SimpleMultiblockData
        get() = simpleMultiblocks.getOrPut(this) { SimpleMultiblockData(null) }

    /**
     * The positions and corresponding components of the multiblock.
     *
     * Any rotation of these components will be considered valid, unless setFacing has been called, in which case
     * only a multiblock constructed facing in the specified direction will be considered valid.
     */
    val components: Map<Vector3i, MultiblockComponent>

    /**
     * Automatically implemented by RebarBlock
     */
    fun getWaila(player: Player): WailaDisplay?

    /**
     * Sets the 'direction' we expect the multiblock to be built in. North is considered the default facing direction -
     * ie setFacing(BlockFace.NORTH) will preserve the original multiblock structure without rotating it.
     *
     * Leave this unset to accept any direction.
     */
    fun setMultiblockDirection(direction: BlockFace?) {
        simpleMultiblockData.direction = direction
    }

    /**
     * The 'direction' we expect the multiblock to be built in. This is not the *actual* direction that
     * the multiblock has been built in.
     */
    fun getMultiblockDirection(): BlockFace?
            = simpleMultiblockData.direction

    /**
     * Returns all the valid configurations of the multiblock. If any of these is satisfied, the multiblock
     * will be considered complete.
     */
    fun validStructures(): List<Map<Vector3i, MultiblockComponent>> {
        val facing = simpleMultiblockData.direction
        return if (facing == null) {
            listOf(
                components,
                rotateComponentsToFace(components, BlockFace.EAST),
                rotateComponentsToFace(components, BlockFace.SOUTH),
                rotateComponentsToFace(components, BlockFace.WEST)
            )
        } else {
            listOf(rotateComponentsToFace(components, facing))
        }
    }

    // Just assumes any rotation of the multiblock is valid, probably not worth the extra logic to account for
    // different facing directions.
    /**
     * Imagine the smallest square containing the multiblock; this is the radius of that square.
     */
    val horizontalRadius
        get() = maxOf(
            abs(components.keys.minOf { it.x }),
            abs(components.keys.minOf { it.z }),
            abs(components.keys.maxOf { it.x }),
            abs(components.keys.maxOf { it.z })
        )

    /**
     * Imagine the smallest cuboid (with equal width and length) containing the multiblock; this is the
     * corner with the lowest X, Y, and Z coordinates.
     */
    val minCorner: Vector3i
        get() = Vector3i(-horizontalRadius, components.keys.minOf { it.y }, -horizontalRadius)

    /**
     * Imagine the smallest cuboid (with equal width and length) containing the multiblock; this is the
     * corner with the highest X, Y, and Z coordinates.
     */
    val maxCorner: Vector3i
        get() = Vector3i(horizontalRadius, components.keys.maxOf { it.y }, horizontalRadius)

    fun getMultiblockBlock(position: Vector3i)
        = block.getRelative(getRotatedPosition(position))

    fun getRotatedPosition(rotatedPosition: Vector3i): Vector3i {
        val direction = getMultiblockDirection()
        return if (direction != null) {
            rotateVectorToFace(rotatedPosition, direction)
        } else {
            rotatedPosition
        }
    }

    fun getMultiblockComponent(position: Vector3i) =
        BlockStorage.get(getMultiblockBlock(position))

    fun <T> getMultiblockComponent(clazz: Class<T>, position: Vector3i) =
        BlockStorage.getAs(clazz, getMultiblockBlock(position))

    fun getMultiblockComponentOrThrow(position: Vector3i) =
        getMultiblockComponent(position) ?: throw IllegalStateException("There is no Rebar block at $position")

    fun <T> getMultiblockComponentOrThrow(clazz: Class<T>, position: Vector3i) =
        getMultiblockComponent(clazz, position) ?: throw IllegalStateException("There is no Rebar block at $position or it is not of type $clazz")

    override val chunksOccupied: Set<ChunkPosition>
        get() {
            val chunks = mutableSetOf<ChunkPosition>()
            for (relativeX in minCorner.x..(maxCorner.x + 16) step 16) {
                val realRelativeX = min(relativeX, maxCorner.x)
                for (relativeZ in minCorner.z..(maxCorner.z + 16) step 16) {
                    val realRelativeZ = min(relativeZ, maxCorner.z)
                    val otherBlock = block.position + Vector3i(realRelativeX, block.y, realRelativeZ)
                    chunks.add(otherBlock.chunk)
                }
            }
            return chunks
        }

    override fun checkFormed(): Boolean {
        // Actual formed checking logic
        val formed = validStructures().any { struct ->
            struct.all {
                it.value.matches(block.location.add(Vector.fromJOML(it.key)).block)
            }
        }

        for ((position, component) in components) {
            component.updateGhostBlock(this, getRotatedPosition(position))
        }

        return formed
    }

    @MustBeInvokedByOverriders
    override fun onMultiblockFormed() {
        for ((position, _) in components) {
            removeGhostBlock(getRotatedPosition(position))
        }
        for (position in components.keys) {
            Waila.addWailaOverride(getMultiblockBlock(position), this::getWaila)
        }
    }

    @MustBeInvokedByOverriders
    override fun onMultiblockUnformed(partUnloaded: Boolean) {
        if (!partUnloaded) {
            for ((position, component) in components) {
                component.spawnGhostBlock(this, getRotatedPosition(position))
            }
        }
        for (position in components.keys) {
            Waila.removeWailaOverride(getMultiblockBlock(position))
        }
    }

    override fun isPartOfMultiblock(otherBlock: Block): Boolean = validStructures().any {
        it.contains((otherBlock.position - block.position).vector3i)
    }

    override val culledEntityIds: Iterable<UUID>
        get() = heldEntities.values

    @ApiStatus.Internal
    companion object : Listener {

        internal data class SimpleMultiblockData(var direction: BlockFace?)

        private val simpleMultiblockKey = rebarKey("simple_multiblock_data")

        private val simpleMultiblocks = IdentityHashMap<RebarSimpleMultiblock, SimpleMultiblockData>()

        @EventHandler
        private fun onPlace(event: RebarBlockPlaceEvent) {
            val block = event.rebarBlock
            if (block !is RebarSimpleMultiblock) return
            for ((position, component) in block.components) {
                component.spawnGhostBlock(block, block.getRotatedPosition(position))
            }
        }

        @EventHandler
        private fun onDeserialize(event: RebarBlockDeserializeEvent) {
            val block = event.rebarBlock
            if (block is RebarSimpleMultiblock) {
                simpleMultiblocks[block] = event.pdc.get(simpleMultiblockKey, RebarSerializers.SIMPLE_MULTIBLOCK_DATA)
                        ?: error("Simple multiblock data not found for ${block.key}")
            }
        }

        @EventHandler
        private fun onSerialize(event: RebarBlockSerializeEvent) {
            val block = event.rebarBlock
            if (block is RebarSimpleMultiblock) {
                event.pdc.set(simpleMultiblockKey, RebarSerializers.SIMPLE_MULTIBLOCK_DATA, simpleMultiblocks[block]!!)
            }
        }

        @EventHandler
        private fun onUnload(event: RebarBlockUnloadEvent) {
            val block = event.rebarBlock
            if (block is RebarSimpleMultiblock) {
                simpleMultiblocks.remove(block)
            }
        }

        @JvmStatic
        fun rotateComponentsToFace(components: Map<Vector3i, MultiblockComponent>, face: BlockFace)
                = components.mapKeys { rotateVectorToFace(it.key, face) }
    }
}