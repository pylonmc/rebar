package io.github.pylonmc.rebar.event

import io.github.pylonmc.rebar.block.PhantomBlock
import io.github.pylonmc.rebar.block.RebarBlock
import org.bukkit.block.Block
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Called when a [RebarBlock] turns into a [PhantomBlock] due to errors or the command.
 */
class RebarBlockPhantomEvent(
    val block: Block,
    val rebarBlock: RebarBlock,
    val phantomBlock: PhantomBlock
) : Event() {

    override fun getHandlers(): HandlerList
        = handlerList

    companion object {
        @JvmStatic
        val handlerList: HandlerList = HandlerList()
    }
}