package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.context.BlockBreakContext
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack

interface RebarBreakHandler {

    /**
     * Called before the block is broken. Note this is not called for [Deletions][BlockBreakContext.Delete]
     * as those are not cancellable.
     *
     * In the case of a vanilla [BlockBreakEvent] this is called at the lowest priority.
     *
     * @return True if the block should be broken, false if the block break should be cancelled.
     */
    fun preBreak(context: BlockBreakContext): Boolean {
        return true
    }

    /**
     * Called as the block is being broken. At this point, the block break cannot be
     * cancelled, but the physical block has not yet been broken or removed from [io.github.pylonmc.rebar.block.BlockStorage].
     *
     * The main purpose of this is method to allow the block drops to be modified.
     *
     * In the case of a vanilla [BlockBreakEvent] this is called during the monitor priority.
     */
    fun onBreak(drops: MutableList<ItemStack>, context: BlockBreakContext) {}

    /**
     * Called after the block has been broken. At this point, the block has been physically
     * removed, block drops have been dropped, and the block has been removed from [io.github.pylonmc.rebar.block.BlockStorage].
     *
     * In the case of a vanilla [BlockBreakEvent] this is called during the monitor priority.
     */
    fun postBreak(context: BlockBreakContext) {}
}