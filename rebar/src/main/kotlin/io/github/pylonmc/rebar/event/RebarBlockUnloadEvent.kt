package io.github.pylonmc.rebar.event

import io.github.pylonmc.rebar.block.RebarBlock
import org.bukkit.block.Block
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Called after the [rebarBlock] has been unloaded.
 */
class RebarBlockUnloadEvent(
    val block: Block,
    val rebarBlock: RebarBlock,
) : Event() {
    override fun getHandlers(): HandlerList
        = handlerList

    companion object {
	    @JvmStatic
        val handlerList: HandlerList = HandlerList()
    }
}