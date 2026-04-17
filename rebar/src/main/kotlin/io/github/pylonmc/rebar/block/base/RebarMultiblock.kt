package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.MultiblockCache
import io.github.pylonmc.rebar.util.position.ChunkPosition
import org.bukkit.block.Block
import org.jetbrains.annotations.ApiStatus


/**
 * Represents a structure composed of multiple blocks.
 *
 * This is an extremely flexible class designed to account for almost any multiblock you could want
 * to create. However, most multiblocks can probably use [RebarSimpleMultiblock].
 *
 * Multiblocks are more difficult than normal Rebar blocks for the simple reason that a multiblock
 * may contain some blocks that have not been loaded because they are in a different chunk.
 *
 * Ticking multiblocks should only tick when `isFormedAndFullyLoaded()` returns true, to avoid
 * ticking a multiblock that is either not formed, or not fully loaded (i.e., not all of its components
 * are loaded).
 *
 * @see RebarSimpleMultiblock
 * @see RebarGhostBlockHolder
 */
interface RebarMultiblock {
    // This is automatically implemented by RebarBlock (lol)
    val block: Block

    /**
     * All chunks containing at least one component of the multiblock
     */
    val chunksOccupied: Set<ChunkPosition>

    /**
     * Warning: You should not call this yourself. If you want to know if the multiblock is formed,
     * use isFormedAndFullyLoaded(), which will read from a cache, so is much faster.
     *
     * You can assume that when this method is called, all components of the multiblock are loaded.
     */
    @ApiStatus.Internal
    fun checkFormed(): Boolean

    /**
     * Returns true if every component of the multiblock is loaded AND the multiblock is formed.
     * Reads from a cache, so call as often as you want.
     */
    fun isFormedAndFullyLoaded(): Boolean
            = MultiblockCache.isFormed(this)

    /**
     * Should return true if there is any scenario in which this block's position could be part
     * of a formed multiblock.
     *
     * This could be called often, so make it lightweight.
     */
    fun isPartOfMultiblock(otherBlock: Block): Boolean

    /**
     * Called when the multiblock is formed (i.e., was not formed before, but now is).
     * This includes when the multiblock was previously formed, unloaded, and loaded again.
     */
    fun onMultiblockFormed() {}

    /**
     * Called when the multiblock is refreshed (i.e., was formed before, and still is).
     * This happens when something causes the Multiblock to re-check if it is formed,
     * and it still is.
     */
    fun onMultiblockRefreshed() {}

    /**
     * Called when the multiblock is unformed (i.e., was formed before, but now is not).
     * This includes when a part of the multiblock is unloaded and the multiblock becomes unformed because of it,
     * or when part of the multiblock is broken/changed, and it becomes unformed because of it.
     *
     * This is **not** called when the multiblock itself is unloaded, use [RebarUnloadBlock] for that.
     *
     * [partUnloaded] is true if the multiblock became unformed because part of it was unloaded, false otherwise.
     */
    fun onMultiblockUnformed(partUnloaded: Boolean) {}
}