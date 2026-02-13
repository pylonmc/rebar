package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.context.BlockBreakContext
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack

interface RebarBreakHandler {
    /**
     * Called before the block is broken. Note this is not called for [Deletions][BlockBreakContext.Delete]
     * as those are not cancellable.
     *
     * In the case of a vanilla [BlockBreakEvent] this is called at the lowest priority
     *
     * @return If the block should be broken. If false, the break is cancelled.
     */
    fun preBreak(context: BlockBreakContext): Boolean {
        return true
    }

    /**
     * In the case of a vanilla [BlockBreakEvent] this is called during the monitor priority
     */
    fun onBreak(drops: MutableList<ItemStack>, context: BlockBreakContext) {}

    /**
     * In the case of a vanilla [BlockBreakEvent] this is called during the monitor priority
     */
    fun postBreak(context: BlockBreakContext) {}
}