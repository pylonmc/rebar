package io.github.pylonmc.rebar.block


import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.addon.RebarAddon
import io.github.pylonmc.rebar.block.BlockStorage.breakBlock
import io.github.pylonmc.rebar.block.base.RebarBreakHandler
import io.github.pylonmc.rebar.block.context.BlockBreakContext
import io.github.pylonmc.rebar.block.context.BlockCreateContext
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.event.*
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.culling.BlockCullingEngine
import io.github.pylonmc.rebar.util.isFromAddon
import io.github.pylonmc.rebar.util.position.BlockPosition
import io.github.pylonmc.rebar.util.position.ChunkPosition
import io.github.pylonmc.rebar.util.position.position
import io.github.pylonmc.rebar.util.rebarKey
import io.papermc.paper.command.brigadier.argument.ArgumentTypes.blockPosition
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.random.Random

/**
 * Welcome to the circus!
 *
 * BlockStorage maintains persistent storage for blocks. Why is this necessary? Due to limitations of
 * Paper/Minecraft, we cannot associate arbitrary data with blocks like we can with entities.
 *
 * BlockStorage guarantees that a chunk's blocks will never be loaded if the chunk is not loaded.
 *
 * We store blocks by chunk, in each chunk's persistent data containers.
 *
 * This works based on chunks rather than individual blocks. When a chunk is loaded, the
 * associated data for all the Rebar blocks in that chunk is loaded. And conversely, when a chunk is
 * unloaded, all the data for that chunk is saved. Additionally, there are autosaves so chunks that
 * are not ever unloaded are still saved occasionally.
 *
 * When saving, we can simply ask the block to write its state to a PDC, then we
 * write that PDC to the chunk. And when loading, we go through each block PDC stored in the chunk,
 * and figure out which block type it is, and then create a new block of that type, using the container
 * to restore the state it had when it was saved.
 *
 * Read AND write access to the loaded block data must be synchronized, as there are multiple fields
 * for loaded blocks. If access is not synchronized, situations may occur where these fields are
 * briefly out of sync. For example, if we unload a chunk, there will be a short delay between
 * deleting the chunk from `blocksByChunk`, and deleting all of its blocks from `blocks`.
 *
 * @see RebarBlock
 */
object BlockStorage : Listener {

    val rebarBlocksKey = rebarKey("blocks")

    // Access to blocks, blocksByChunk, blocksById fields must be synchronized
    // to prevent them briefly going out of sync
    private val blockLock = ReentrantReadWriteLock()

    private val blocks: MutableMap<BlockPosition, RebarBlock> = ConcurrentHashMap()

    // Only contains chunks that have been loaded (including chunks with no Rebar blocks)
    private val blocksByChunk: MutableMap<ChunkPosition, MutableList<RebarBlock>> = ConcurrentHashMap()

    private val blocksByKey: MutableMap<NamespacedKey, MutableList<RebarBlock>> = ConcurrentHashMap()

    private val chunkAutosaveTasks: MutableMap<ChunkPosition, Job> = ConcurrentHashMap()

    @JvmStatic
    val loadedBlockPositions: Set<BlockPosition>
        get() = lockBlockRead { blocks.keys }

    @JvmStatic
    val loadedChunks: Set<ChunkPosition>
        get() = lockBlockRead { blocksByChunk.keys }

    @JvmStatic
    val loadedRebarBlocks: Collection<RebarBlock>
        get() = lockBlockRead { blocks.values }

    /**
     * Returns the Rebar block at the given [blockPosition], or null if the block does not exist
     *
     * @throws IllegalArgumentException if the chunk containing the block is not loaded
     */
    @JvmStatic
    fun get(blockPosition: BlockPosition): RebarBlock? {
        require(blockPosition.chunk.isLoaded) { "You can only get Rebar blocks in loaded chunks" }
        return lockBlockRead { blocks[blockPosition] }
    }

    /**
     * Returns the Rebar block at the given [block], or null if the block does not exist.
     *
     * @throws IllegalArgumentException if the chunk containing the block is not loaded
     */
    @JvmStatic
    fun get(block: Block): RebarBlock? = get(block.position)

    /**
     * Returns the Rebar block at the given [location], or null if the block does not exist.
     *
     * @throws IllegalArgumentException if the chunk containing the block is not loaded
     */
    @JvmStatic
    fun get(location: Location): RebarBlock? = get(location.block)

    /**
     * Returns the Rebar block (of type [T]) at the given [blockPosition], or null if the block
     * does not exist or is not of the expected class.
     *
     * @throws IllegalArgumentException if the chunk containing the block is not loaded
     */
    @JvmStatic
    fun <T> getAs(clazz: Class<T>, blockPosition: BlockPosition): T? {
        val block = get(blockPosition) ?: return null
        if (!clazz.isInstance(block)) {
            return null
        }
        return clazz.cast(block)
    }

    /**
     * Returns the Rebar block (of type [T]) at the given [block], or null if the block
     * does not exist or is not of the expected class.
     *
     * @throws IllegalArgumentException if the chunk containing the block is not loaded
     */
    @JvmStatic
    fun <T> getAs(clazz: Class<T>, block: Block): T? = getAs(clazz, block.position)

    /**
     * Returns the Rebar block (of type [T]) at the given [location], or null if the block
     * does not exist or is not of the expected class.
     *
     * @throws IllegalArgumentException if the chunk containing the block is not loaded
     */
    @JvmStatic
    fun <T> getAs(clazz: Class<T>, location: Location): T? =
        getAs(clazz, BlockPosition(location))

    /**
     * Gets the Rebar block (of type [T]) at the given [blockPosition].
     *
     * Returns null if the block does not exist or is not of the expected class.
     *
     * @throws IllegalArgumentException if the chunk containing the block is not loaded
     */
    inline fun <reified T> getAs(blockPosition: BlockPosition): T? =
        getAs(T::class.java, blockPosition)

    /**
     * Returns the Rebar block (of type [T]) at the given [block].
     *
     * Returns null if the block does not exist or is not of the expected class.
     *
     * @throws IllegalArgumentException if the chunk containing the block is not loaded
     */
    inline fun <reified T> getAs(block: Block): T? = getAs(T::class.java, block)

    /**
     * Returns the Rebar block (of type [T]) at the given [location].
     *
     * Returns null if the block does not exist or is not of the expected class.
     *
     * @throws IllegalArgumentException if the chunk containing the block is not loaded
     */
    inline fun <reified T> getAs(location: Location): T? = getAs(T::class.java, location)

    /**
     * Returns all the Plyon blocks in the chunk at [chunkPosition].
     *
     * @throws IllegalArgumentException if the chunk is not loaded
     */
    @JvmStatic
    fun getByChunk(chunkPosition: ChunkPosition): Collection<RebarBlock> {
        require(chunkPosition.isLoaded) { "You can only get Rebar blocks in loaded chunks" }
        return lockBlockRead { blocksByChunk[chunkPosition].orEmpty() }
    }

    /**
     * Returns all the Plyon blocks with type [key].
     */
    @JvmStatic
    fun getByKey(key: NamespacedKey): Collection<RebarBlock> =
        if (RebarRegistry.BLOCKS.contains(key)) {
            lockBlockRead {
                blocksByKey[key].orEmpty()
            }
        } else {
            emptySet()
        }

    /**
     * Returns whether the block at [blockPosition] is a Rebar block, or null if the
     * chunk at [blockPosition] is not loaded
     */
    @JvmStatic
    fun isRebarBlock(blockPosition: BlockPosition): Boolean =
        (blockPosition.chunk.isLoaded) && get(blockPosition) != null

    /**
     * Returns whether the block at [block] is a Rebar block, or null if the
     * chunk at [block] is not loaded
     */
    @JvmStatic
    fun isRebarBlock(block: Block): Boolean =
        (block.position.chunk.isLoaded) && get(block) != null

    /**
     * Creates a new Rebar block. Only call on the main thread.
     *
     * @return The block that was placed, or null if the block placement was cancelled
     *
     * @throws IllegalArgumentException if the chunk of the given [block] is not
     * loaded, the block already contains a Rebar block, or the block type given by
     * [key] does not exist.
     */
    @JvmStatic
    @JvmOverloads
    fun placeBlock(
        block: Block,
        key: NamespacedKey,
        context: BlockCreateContext = BlockCreateContext.Default(block)
    ) = placeBlock(block.position, key, context)

    /**
     * Creates a new Rebar block. Only call on the main thread.
     *
     * @return The block that was placed, or null if the block placement was cancelled
     *
     * @throws IllegalArgumentException if the chunk of the given [location] is not
     * loaded, the block already contains a Rebar block, or the block type given by
     * [key] does not exist.
     */
    @JvmStatic
    @JvmOverloads
    fun placeBlock(
        location: Location,
        key: NamespacedKey,
        context: BlockCreateContext = BlockCreateContext.Default(location.block)
    ) = placeBlock(BlockPosition(location), key, context)

    /**
     * Creates a new Rebar block. Only call on the main thread.
     *
     * @return The block that was placed, or null if the block placement was cancelled
     *
     * @throws IllegalArgumentException if the chunk of the given [blockPosition] is not
     * loaded, the block already contains a Rebar block, or the block type given by
     * [key] does not exist.
     */
    @JvmStatic
    @JvmOverloads
    fun placeBlock(
        blockPosition: BlockPosition,
        key: NamespacedKey,
        context: BlockCreateContext = BlockCreateContext.Default(blockPosition.block)
    ): RebarBlock? {
        require(blockPosition.chunk.isLoaded) { "You can only place Rebar blocks in loaded chunks" }
        require(!isRebarBlock(blockPosition)) { "You cannot place a new Rebar block in place of an existing Rebar blocks" }

        val schema = RebarRegistry.BLOCKS[key]
        require(schema != null) { "Block $key does not exist" }

        if (!PreRebarBlockPlaceEvent(blockPosition.block, schema, context).callEvent()) return null

        return setBlock(blockPosition, schema, context)
    }

    @JvmSynthetic
    internal fun setBlock(
        blockPosition: BlockPosition,
        schema: RebarBlockSchema,
        context: BlockCreateContext
    ) : RebarBlock {
        if (context.shouldSetType) {
            blockPosition.block.type = schema.material
        }

        @Suppress("UNCHECKED_CAST") // The cast will work - this is checked in the schema constructor
        val block = schema.create(blockPosition.block, context)

        lockBlockWrite {
            check(blockPosition.chunk in blocksByChunk) { "Chunk '${blockPosition.chunk}' must be loaded" }
            blocks[blockPosition] = block
            blocksByKey.getOrPut(schema.key, ::mutableListOf).add(block)
            blocksByChunk[blockPosition.chunk]!!.add(block)
        }

        RebarBlockPlaceEvent(blockPosition.block, block, context).callEvent()
        block.postInitialise()
        BlockCullingEngine.insert(block)

        return block
    }

    /**
     * Manually loads Rebar block data at a location from a persistent data container that contains the serialised block. Only call on the main thread.
     *
     * @return The block that was loaded, or null if the block loading was cancelled
     *
     * @throws IllegalArgumentException if the chunk of the given [blockPosition] is not
     * loaded, the block already contains a Rebar block
     */
    @JvmStatic
    fun loadBlock(
        blockPosition: BlockPosition,
        schema: RebarBlockSchema,
        pdcData: PersistentDataContainer
    ): RebarBlock? {
        val context = BlockCreateContext.ManualLoading(blockPosition.block)
        val block = blockPosition.block
        pdcData.set(RebarBlock.rebarBlockPositionKey, PersistentDataType.LONG, blockPosition.asLong)

        require(block.chunk.isLoaded) { "You can only place Rebar blocks in loaded chunks" }
        require(!isRebarBlock(block)) { "You cannot place a new Rebar block in place of an existing Rebar blocks" }

        if (!PreRebarBlockPlaceEvent(block, schema, context).callEvent()) return null
        if (context.shouldSetType) {
            block.type = schema.material
        }

        val pyBlock = RebarBlock.deserialize(block.world, pdcData)!!

        lockBlockWrite {
            check(blockPosition.chunk in blocksByChunk) { "Chunk '${blockPosition.chunk}' must be loaded" }
            blocks[blockPosition] = pyBlock
            blocksByKey.getOrPut(schema.key, ::mutableListOf).add(pyBlock)
            blocksByChunk[blockPosition.chunk]!!.add(pyBlock)
        }

        RebarBlockPlaceEvent(block, pyBlock, context).callEvent()
        BlockCullingEngine.insert(pyBlock)

        return pyBlock
    }

    /**
     * Removes a Rebar block and breaks the physical block in the world.
     * Does nothing if the block is not a Rebar block.
     * Only call on the main thread.
     *
     * @return The items that were dropped by the block being broken
     *
     * @throws IllegalArgumentException if the chunk of the given [blockPosition] is not
     * loaded.
     */
    @JvmStatic
    @JvmOverloads
    fun breakBlock(
        blockPosition: BlockPosition,
        context: BlockBreakContext = BlockBreakContext.PluginBreak(blockPosition.block)
    ): List<Item>? {
        require(blockPosition.chunk.isLoaded) { "You can only break Rebar blocks in loaded chunks" }
        val block = get(blockPosition) ?: return null
        if (!preBreakBlock(block, context)) return null
        return removeBlock(block, blockPosition, context)
    }

    @JvmSynthetic
    internal fun preBreakBlock(
        block: RebarBlock,
        context: BlockBreakContext
    ) : Boolean {
        if (block is RebarBreakHandler && !block.preBreak(context)) {
            return false
        }
        return PreRebarBlockBreakEvent(block.block, block, context).callEvent()
    }

    @JvmSynthetic
    internal fun removeBlock(
        block: RebarBlock,
        blockPosition: BlockPosition,
        context: BlockBreakContext
    ) : List<Item> {
        val drops = mutableListOf<ItemStack>()
        if (context.normallyDrops) {
            block.getDropItem(context)?.let { drops.add(it.clone()) }
        }
        if (block is RebarBreakHandler) {
            block.onBreak(drops, context)
        }

        lockBlockWrite {
            blocks.remove(blockPosition)
            blocksByKey[block.schema.key]?.remove(block)
            blocksByChunk[blockPosition.chunk]?.remove(block)
        }

        if (context.shouldSetToAir) {
            blockPosition.block.type = Material.AIR
        }
        if (block is RebarBreakHandler) {
            block.postBreak(context)
        }

        BlockCullingEngine.remove(block)
        RebarBlockBreakEvent(blockPosition.block, block, context, drops).callEvent()

        val droppedItems = mutableListOf<Item>()
        val dropLocation = block.block.location.add(0.5, 0.1, 0.5)
        for (drop in drops) {
            droppedItems.add(block.block.world.dropItemNaturally(dropLocation, drop))
        }

        return Collections.unmodifiableList(droppedItems)
    }

    /**
     * Removes a Rebar block and breaks the physical block in the world.
     * Does nothing if the block is not a Rebar block.
     * Only call on the main thread.
     *
     * @return The items that were dropped by the block being broken
     *
     * @throws IllegalArgumentException if the chunk of the given [block] is not
     * loaded.
     */
    @JvmStatic
    @JvmOverloads
    fun breakBlock(block: Block, context: BlockBreakContext = BlockBreakContext.PluginBreak(block)) =
        breakBlock(block.position, context)

    /**
     * Removes a Rebar block and breaks the physical block in the world.
     * Does nothing if the block is not a Rebar block.
     * Only call on the main thread.
     *
     * @return The items that were dropped by the block being broken
     *
     * @throws IllegalArgumentException if the chunk of the given [block] is not
     * loaded.
     */
    @JvmStatic
    @JvmOverloads
    fun breakBlock(block: RebarBlock, context: BlockBreakContext = BlockBreakContext.PluginBreak(block.block)) =
        breakBlock(block.block, context)

    /**
     * Removes a Rebar block and breaks the physical block in the world.
     * Does nothing if the block is not a Rebar block.
     * Only call on the main thread.
     *
     * @return The items that were dropped by the block being broken
     *
     * @throws IllegalArgumentException if the chunk of the given [location] is not
     * loaded.
     */
    @JvmStatic
    @JvmOverloads
    fun breakBlock(location: Location, context: BlockBreakContext = BlockBreakContext.PluginBreak(location.block)) =
        breakBlock(BlockPosition(location), context)

    /**
     * Deletes the Rebar block and removes the physical block in the world.
     * Does nothing if the block is not a Rebar block.
     * Only call on the main thread.
     *
     * This differs from [breakBlock] in that it cannot be cancelled and does not drop any items.
     */
    @JvmSynthetic
    internal fun deleteBlock(blockPosition: BlockPosition) {
        require(blockPosition.chunk.isLoaded) { "You can only delete Rebar block data in loaded chunks" }

        val block = get(blockPosition) ?: return

        val context = BlockBreakContext.Delete(block.block)
        if (block is RebarBreakHandler) {
            block.onBreak(mutableListOf(), context)
        }

        lockBlockWrite {
            blocks.remove(blockPosition)
            blocksByKey[block.schema.key]?.remove(block)
            blocksByChunk[blockPosition.chunk]?.remove(block)
        }

        block.block.type = Material.AIR
        if (block is RebarBreakHandler) {
            block.postBreak(context)
        }

        BlockCullingEngine.remove(block)
        RebarBlockBreakEvent(blockPosition.block, block, context, mutableListOf()).callEvent()
    }

    private fun load(world: World, chunk: Chunk): List<RebarBlock> {
        val type = RebarSerializers.LIST.listTypeFrom(RebarSerializers.TAG_CONTAINER)
        val chunkBlocks = chunk.persistentDataContainer.get(rebarBlocksKey, type)?.mapNotNull { element ->
            RebarBlock.deserialize(world, element)
        }?.toMutableList() ?: mutableListOf()

        return chunkBlocks
    }

    private fun save(chunk: Chunk, chunkBlocks: MutableList<RebarBlock>) {
        val serializedBlocks = chunkBlocks.map {
            RebarBlock.serialize(it, chunk.persistentDataContainer.adapterContext)
        }

        val type = RebarSerializers.LIST.listTypeFrom(RebarSerializers.TAG_CONTAINER)
        chunk.persistentDataContainer.set(rebarBlocksKey, type, serializedBlocks)
    }

    @EventHandler
    private fun onChunkLoad(event: ChunkLoadEvent) {
        val chunkBlocks = load(event.world, event.chunk)

        lockBlockWrite {
            blocksByChunk[event.chunk.position] = chunkBlocks.toMutableList()
            for (block in chunkBlocks) {
                blocks[block.block.position] = block
                blocksByKey.computeIfAbsent(block.schema.key) { mutableListOf() }.add(block)
            }

            // autosaving
            chunkAutosaveTasks[event.chunk.position] = Rebar.launch(Rebar.minecraftDispatcher) {

                // Wait a random delay before starting, this is to help smooth out lag from saving
                delay(Random.nextLong(RebarConfig.BLOCK_DATA_AUTOSAVE_INTERVAL_SECONDS * 1000))

                while (true) {
                    lockBlockRead {
                        val blocksInChunk = blocksByChunk[event.chunk.position]
                        check(blocksInChunk != null) { "Block autosave task was not cancelled properly" }
                        save(event.chunk, blocksInChunk)
                    }
                    delay(RebarConfig.BLOCK_DATA_AUTOSAVE_INTERVAL_SECONDS * 1000)
                }
            }
        }

        for (block in chunkBlocks) {
            RebarBlockLoadEvent(block.block, block).callEvent()
            BlockCullingEngine.insert(block)
        }

        RebarChunkBlocksLoadEvent(event.chunk, chunkBlocks.toList()).callEvent()
    }

    @EventHandler
    private fun onChunkUnload(event: ChunkUnloadEvent) {
        val chunkBlocks = lockBlockWrite {
            val chunkBlocks = blocksByChunk.remove(event.chunk.position)
                ?: error("Attempted to save Rebar data for chunk '${event.chunk.position}' but no data is stored")
            for (block in chunkBlocks) {
                blocks.remove(block.block.position)
                (blocksByKey[block.schema.key] ?: continue).remove(block)
            }
            chunkAutosaveTasks.remove(event.chunk.position)?.cancel()
            chunkBlocks
        }

        save(event.chunk, chunkBlocks)

        for (block in chunkBlocks) {
            BlockCullingEngine.remove(block)
            RebarBlockUnloadEvent(block.block, block).callEvent()
        }

        RebarChunkBlocksUnloadEvent(event.chunk, chunkBlocks.toList()).callEvent()
    }

    /**
     * Unloads blocks from a specific addon.
     * This doesn't actually delete them from memory, but instead converts them into
     * PhantomBlocks so that they are saved. See PhantomBlock for more info.
     */
    @JvmSynthetic
    internal fun cleanup(addon: RebarAddon) = lockBlockWrite {
        val replacer: (RebarBlock) -> RebarBlock = { block ->
            if (block.schema.key.isFromAddon(addon)) {
                RebarBlockSchema.schemaCache[block.block.position] = PhantomBlock.schema
                PhantomBlock(
                    RebarBlock.serialize(block, block.block.chunk.persistentDataContainer.adapterContext),
                    block.schema.key,
                    block.block
                )
            } else {
                block
            }
        }

        blocks.replaceAll { _, block -> replacer.invoke(block) }
        for ((key, blocks) in blocksByKey) {
            val iter = blocks.listIterator()
            while (iter.hasNext()) {
                val block = iter.next()
                try {
                    iter.set(replacer.invoke(block))
                } catch (e: Exception) {
                    Rebar.logger.severe("Exception while cleaning up block at ${block.block.position} (while cleaning up ${addon.key})")
                    e.printStackTrace()
                    iter.remove()
                }
            }
            blocks.replaceAll(replacer)
        }
        for (blocks in blocksByChunk.values) {
            blocks.replaceAll(replacer)
        }
    }

    /**
     * Turns the block into a [PhantomBlock] which represents a block which has failed for some reason
     */
    @JvmSynthetic
    internal fun makePhantom(block: RebarBlock) = lockBlockWrite {
        BlockCullingEngine.remove(block)
        RebarBlockSchema.schemaCache[block.block.position] = PhantomBlock.schema
        val phantomBlock = PhantomBlock(
            RebarBlock.serialize(block, block.block.chunk.persistentDataContainer.adapterContext),
            block.schema.key,
            block.block
        )

        blocks.replace(block.block.position, block, phantomBlock)
        blocksByKey[block.key]!!.remove(block)
        blocksByKey[block.key]!!.add(phantomBlock)
        blocksByChunk[block.block.chunk.position]!!.remove(block)
        blocksByChunk[block.block.chunk.position]!!.add(phantomBlock)
    }

    @JvmSynthetic
    internal fun cleanupEverything() {
        for ((chunkPosition, chunkBlocks) in blocksByChunk) {
            save(chunkPosition.chunk!!, chunkBlocks)
        }
    }

    private inline fun <T> lockBlockRead(block: () -> T): T {
        blockLock.readLock().lock()
        try {
            return block()
        } finally {
            blockLock.readLock().unlock()
        }
    }

    private inline fun <T> lockBlockWrite(block: () -> T): T {
        blockLock.writeLock().lock()
        try {
            return block()
        } finally {
            blockLock.writeLock().unlock()
        }
    }
}