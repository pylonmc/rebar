package io.github.pylonmc.rebar.block.base

import com.github.shynixn.mccoroutine.bukkit.launch
import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.block.MultiblockCache
import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.block.RebarBlockSchema
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.entity.RebarEntity
import io.github.pylonmc.rebar.entity.display.BlockDisplayBuilder
import io.github.pylonmc.rebar.entity.display.ItemDisplayBuilder
import io.github.pylonmc.rebar.entity.display.transform.TransformBuilder
import io.github.pylonmc.rebar.event.RebarBlockDeserializeEvent
import io.github.pylonmc.rebar.event.RebarBlockPlaceEvent
import io.github.pylonmc.rebar.event.RebarBlockSerializeEvent
import io.github.pylonmc.rebar.event.RebarBlockUnloadEvent
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.getrelative
import io.github.pylonmc.rebar.util.position.ChunkPosition
import io.github.pylonmc.rebar.util.position.position
import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.util.rotateVectorToFace
import io.github.pylonmc.rebar.waila.WailaDisplay
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Display
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.util.Vector
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.MustBeInvokedByOverriders
import org.joml.Vector3i
import java.util.IdentityHashMap
import java.util.UUID
import kotlin.math.abs
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds

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
interface RebarSimpleMultiblock : RebarMultiblock, RebarEntityHolderBlock, RebarEntityCulledBlock {

    /**
     * Implement this together with [MultiblockComponent], it is used to spawn a single entity
     * to display the needs of a single multiblock requirement
     *
     * You must implement either this or [MultipleGhostBlocks], together with [MultiblockComponent] for a correct implementation
     */
    interface SingleGhostBlock {
        fun spawnGhostBlock(block: Block): UUID
    }

    /**
     * Implement this together with [MultiblockComponent], it is used to spawn multiple entities
     * to display the needs of a single multiblock requirement, you are going to need this only when
     * you can't display a block requirement with only a single [ItemDisplay] or [BlockDisplay]
     *
     * You must implement either this or [SingleGhostBlock], together with [MultiblockComponent] for a correct implementation
     */
    interface MultipleGhostBlocks {
        fun spawnGhostBlocks(block: Block): List<UUID>
    }

    /**
     * Represents a single block of a multiblock.
     */
    interface MultiblockComponent {
        fun matches(block: Block): Boolean

        companion object {

            @JvmStatic
            fun of(material: Material) = VanillaMultiblockComponent(material)

            @JvmStatic
            fun of(key: NamespacedKey) = RebarMultiblockComponent(key)
        }
    }

    interface MultiblockComponentBlockDisplay {
        fun blockDataList() : List<BlockData>
    }

    /**
     * A block display that represents this block, showing the player what block
     * needs to be placed in a specific location.
     */
    class MultiblockGhostBlock(entity: Display, val displayName: Component) :
        RebarEntity<Display>(KEY, entity) {

        constructor(entity: Display, displayName: String)
                : this(entity, Component.text(displayName))

        constructor(entity: Display)
                : this(entity, entity.persistentDataContainer.get(NAME_KEY, RebarSerializers.COMPONENT)!!)

        override fun getWaila(player: Player): WailaDisplay {
            return WailaDisplay(displayName)
        }

        override fun write(pdc: PersistentDataContainer) {
            pdc.set(NAME_KEY, RebarSerializers.COMPONENT, displayName)
        }

        companion object {
            val KEY = rebarKey("multiblock_ghost_block")
            val NAME_KEY = rebarKey("display_name")
        }
    }

    /**
     * Represents a vanilla component of a multiblock, which can have one or more materials.
     *
     * If multiple materials are specified, the ghost block will automatically cycle through all
     * the given materials in order.
     */
    @JvmRecord
    data class VanillaMultiblockComponent(val materials: List<Material>) : SingleGhostBlock, MultiblockComponent, MultiblockComponentBlockDisplay {

        constructor(first: Material, vararg materials: Material) : this(listOf(first) + materials)

        init {
            check(materials.isNotEmpty()) { "Materials list cannot be empty" }
        }

        override fun matches(block: Block): Boolean = !BlockStorage.isRebarBlock(block) && block.type in materials

        override fun spawnGhostBlock(block: Block): UUID {
            val blockDataList = blockDataList()
            val display = BlockDisplayBuilder()
                .material(materials.first())
                .glow(Color.WHITE)
                .transformation(TransformBuilder().scale(0.5))
                .displayWidth(0.5f)
                .displayHeight(0.5f)
                .build(block.location.toCenterLocation())
            EntityStorage.add(MultiblockGhostBlock(display, materials.joinToString(", ") { it.key.toString() }))

            if (materials.size > 1) {
                Rebar.launch {
                    var i = 0
                    while (display.isValid) {
                        display.block = blockDataList[i]
                        i++
                        i %= materials.size
                        delay(1.seconds)
                    }
                }
            }

            return display.uniqueId
        }

        override fun blockDataList(): List<BlockData> = materials.map { it.createBlockData() }
    }

    /**
     * Represents a vanilla component of a multiblock, which can have one or more blockdatas.
     *
     * If multiple blockdatas are specified, the ghost block will automatically cycle through all
     * the given blockdatas in order.
     *
     * This should be used only when you want to impose some constraints about the blockdata, for instance:
     *
     * <pre>{@code
     * BlockData data = Material.CAMPFIRE.createBlockData("[lit=true]") // requires the campfire to be lit
     * new VanillaBlockdataMultiblockComponent(data);
     *
     * // or if you prefer
     * Campfire fire = (Campfire) Material.CAMPFIRE.createBlockData();
     * fire.setLit(true);
     * new VanillaBlockdataMultiblockComponent(fire);
     * }
     * </pre>
     *
     */
    @JvmRecord
    data class VanillaBlockdataMultiblockComponent(val blockDatas: List<BlockData>) : SingleGhostBlock, MultiblockComponent, MultiblockComponentBlockDisplay {

        constructor(first: BlockData, vararg materials: BlockData) : this(listOf(first) + materials)

        init {
            check(blockDatas.isNotEmpty()) { "BlockData list cannot be empty" }
        }

        override fun matches(block: Block): Boolean {
            if (BlockStorage.isRebarBlock(block)) return false
            for (blockData in blockDatas) {
                // IMPORTANT, a.matches(b) != b.matches(a), if you invert this check, kaboom
                if (block.blockData.matches(blockData)) return true
            }

            return false
        }

        override fun spawnGhostBlock(block: Block): UUID {
            val stringDatas: List<String> = blockDatas.map { it.getAsString(true) }
            val display = BlockDisplayBuilder()
                .material(blockDatas.first().material)
                .glow(Color.WHITE)
                .transformation(TransformBuilder().scale(0.5))
                .build(block.location.toCenterLocation())
            EntityStorage.add(MultiblockGhostBlock(display, stringDatas.joinToString(", ")))

            if (blockDatas.size > 1) {
                Rebar.launch {
                    var i = 0
                    while (display.isValid) {
                        display.block = blockDatas[i]
                        i++
                        i %= blockDatas.size
                        delay(1.seconds)
                    }
                }
            }

            return display.uniqueId
        }

        override fun blockDataList(): List<BlockData> = blockDatas
    }

    /**
     * Displays all kind of MultiblockComponents that implement [MultiblockComponentBlockDisplay], together with
     * special item handling for [RebarMultiblockComponent]
     */
    class MixedMultiblockComponent : MultiblockComponent, MultipleGhostBlocks {
        val multiblockComponents: Collection<MultiblockComponent>

        constructor(multiblockComponents: Collection<MultiblockComponent>) {
            this.multiblockComponents = multiblockComponents
        }

        constructor(vararg validators: MultiblockComponent) : this(validators.toList())

        override fun matches(block: Block): Boolean {
            for (validator in multiblockComponents) {
                if (validator.matches(block)) {
                    return true
                }
            }

            return false
        }

        override fun spawnGhostBlocks(block: Block): List<UUID> {
            var blockDisplay: BlockDisplay? = null
            var itemDisplay: ItemDisplay? = null

            val displayUpdates: MutableList<Runnable> = mutableListOf()
            var name = ""
            for (component in multiblockComponents) {
                if (component is MultiblockComponentBlockDisplay) {
                    val blockDatas = component.blockDataList()
                    if (blockDisplay == null) {
                        blockDisplay = BlockDisplayBuilder()
                            .material(blockDatas.first().material)
                            .glow(Color.WHITE)
                            .transformation(TransformBuilder().scale(0.5))
                            .build(block.location.toCenterLocation())
                    }

                    for (blockData in blockDatas) {
                        displayUpdates.add {
                            blockDisplay.isVisibleByDefault = true
                            itemDisplay?.isVisibleByDefault = false
                            blockDisplay.block = blockData
                        }

                        name += blockData.getAsString(true) + ", "
                    }
                } else if (component is RebarMultiblockComponent) {
                    val key = component.key
                    val schema = component.schema()
                    val itemBuilder = ItemStackBuilder.of(schema.material).addCustomModelDataString(key.toString())
                    if (itemDisplay == null) {
                        itemDisplay = ItemDisplayBuilder()
                            .itemStack(itemBuilder)
                            .glow(Color.WHITE)
                            .transformation(TransformBuilder().scale(0.5))
                            .build(block.location.toCenterLocation())
                    }

                    displayUpdates.add {
                        itemDisplay.isVisibleByDefault = true
                        blockDisplay?.isVisibleByDefault = false
                        itemDisplay.setItemStack(
                            itemBuilder.build()
                        )
                    }

                    name += "$key, "
                }
            }

            blockDisplay?.let {
                EntityStorage.add(MultiblockGhostBlock(it, name))
            }

            itemDisplay?.let {
                EntityStorage.add(MultiblockGhostBlock(it, name))
            }

            if (displayUpdates.size > 1) {
                Rebar.launch {
                    var i = 0
                    while (itemDisplay?.isValid ?: true && blockDisplay?.isValid ?: true) {
                        displayUpdates[i].run()
                        i++
                        i %= displayUpdates.size
                        delay(1.seconds)
                    }
                }
            }

            val mutableList = mutableListOf<UUID>()
            blockDisplay?.let { mutableList.add(it.uniqueId) }
            itemDisplay?.let { mutableList.add(it.uniqueId) }

            return mutableList
        }
    }

    /**
     * Represents a Rebar block component of a multiblock.
     */
    @JvmRecord
    data class RebarMultiblockComponent(val key: NamespacedKey) : SingleGhostBlock, MultiblockComponent {
        fun schema() : RebarBlockSchema = RebarRegistry.BLOCKS[key]
            ?: throw IllegalArgumentException("Block schema $key does not exist")

        override fun matches(block: Block): Boolean = BlockStorage.get(block)?.schema?.key == key

        override fun spawnGhostBlock(block: Block): UUID {
            val schema = schema()
            val display = ItemDisplayBuilder()
                .itemStack(ItemStackBuilder.of(schema.material).addCustomModelDataString(key.toString()))
                .glow(Color.WHITE)
                .transformation(TransformBuilder().scale(0.5))
                .build(block.location.toCenterLocation())
            EntityStorage.add(MultiblockGhostBlock(display, key.toString()))
            return display.uniqueId
        }
    }

    @get:ApiStatus.NonExtendable
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

    /**
     * Spawns a ghost block for every component of the multiblock.
     */
    @ApiStatus.Internal
    fun spawnGhostBlocks() {
        val block = (this as RebarBlock).block
        val facing = simpleMultiblockData.direction
        val rotatedComponents = if (facing == null) components else rotateComponentsToFace(components, facing)
        for ((offset, component) in rotatedComponents) {
            val startSection = "multiblock_ghost_block_${offset.x}_${offset.y}_${offset.z}"

            if (component is SingleGhostBlock) {
                if (!isHeldEntityPresent(startSection)) {
                    val ghostBlock = component.spawnGhostBlock((block.position + offset).block)
                    heldEntities[startSection] = ghostBlock
                }
            } else if (component is MultipleGhostBlocks) {
                if (!heldEntities.keys.any { it.startsWith(startSection) }) {
                    val ghostBlocks = component.spawnGhostBlocks((block.position + offset).block)

                    var i = 0
                    ghostBlocks.forEach { ghostBlock ->
                        val key = "${startSection}_$i"
                        i++
                        heldEntities[key] = ghostBlock
                    }
                }
            }
        }
        updateGhostBlockColors()
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

    fun getMultiblockBlock(position: Vector3i): Block {
        check(position in components.keys) { "Block at $position is not part of the multiblock" }
        val direction = getMultiblockDirection()
        return if (direction != null) {
            block.getrelative(rotateVectorToFace(position, direction))
        } else {
            block.getrelative(position)
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

        updateGhostBlockColors()

        return formed
    }

    @MustBeInvokedByOverriders
    override fun onMultiblockFormed() {
        val toRemove = heldEntities.keys.filter { it.startsWith("multiblock_ghost_block_") }
        for (key in toRemove) {
            EntityStorage.get(heldEntities[key]!!)!!.entity.remove()
            heldEntities.remove(key)
        }
    }

    @MustBeInvokedByOverriders
    override fun onMultiblockUnformed(partUnloaded: Boolean) {
        if (!partUnloaded) {
            spawnGhostBlocks()
        }
    }

    override fun isPartOfMultiblock(otherBlock: Block): Boolean = validStructures().any {
        it.contains((otherBlock.position - block.position).vector3i)
    }

    override val culledEntityIds: Iterable<UUID>
        get() = heldEntities.values

    /**
     * Updates the color of all ghost blocks to indicate whether the block is correctly placed.
     */
    @ApiStatus.Internal
    fun updateGhostBlockColors() {
        if (MultiblockCache.isFormed(this)) {
            return // ghosts should have been deleted
        }

        val block = (this as RebarBlock).block
        val facing = simpleMultiblockData.direction
        val rotatedComponents = if (facing == null) components else rotateComponentsToFace(components, facing)
        for ((offset, component) in rotatedComponents) {
            val mainKey = "multiblock_ghost_block_${offset.x}_${offset.y}_${offset.z}"
            val entity = getHeldRebarEntity(
                MultiblockGhostBlock::class.java,
                "multiblock_ghost_block_${offset.x}_${offset.y}_${offset.z}"
            )
            if (entity != null) {
                entity.entity.glowColorOverride = if (component.matches((block.position + offset).block)) {
                    Color.GREEN
                } else {
                    Color.RED
                }
            } else {
                for (entry in heldEntities.entries) {
                    if (!entry.key.startsWith(mainKey)) continue

                    val entityEntry = getHeldRebarEntity(
                        MultiblockGhostBlock::class.java,
                        entry.key
                    )

                    if (entityEntry == null) continue

                    entityEntry.entity.glowColorOverride = if (component.matches((block.position + offset).block)) {
                        Color.GREEN
                    } else {
                        Color.RED
                    }
                }
            }
        }
    }

    @ApiStatus.Internal
    companion object : Listener {

        internal data class SimpleMultiblockData(var direction: BlockFace?)

        private val simpleMultiblockKey = rebarKey("simple_multiblock_data")

        private val simpleMultiblocks = IdentityHashMap<RebarSimpleMultiblock, SimpleMultiblockData>()

        @EventHandler
        private fun onPlace(event: RebarBlockPlaceEvent) {
            val block = event.rebarBlock
            if (block !is RebarSimpleMultiblock) return
            block.spawnGhostBlocks()
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