package io.github.pylonmc.rebar.event

import io.github.pylonmc.rebar.block.RebarBlock
import org.bukkit.block.Block
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.persistence.PersistentDataContainer

/**
 * Called after the [rebarBlock] is serialized. **A block being serialized does not necessarily mean
 * it is going to be unloaded.**
 */
class RebarBlockSerializeEvent(
    val block: Block,
    val rebarBlock: RebarBlock,
    val pdc: PersistentDataContainer,
    val debug: Boolean,
) : Event() {

    override fun getHandlers(): HandlerList = handlerList

    companion object {
        @JvmStatic
        val handlerList: HandlerList = HandlerList()
    }
}