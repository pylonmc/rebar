package io.github.pylonmc.rebar.item.interfaces

import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import io.github.pylonmc.rebar.item.research.Research.Companion.canUse
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerBucketFillEvent
import org.jetbrains.annotations.ApiStatus

interface BucketRebarItemHandler {
    /**
     * Called when the bucket is emptied.
     */
    fun onBucketEmpty(event: PlayerBucketEmptyEvent, priority: EventPriority) {}

    /**
     * Called when the bucket is filled.
     */
    fun onBucketFill(event: PlayerBucketFillEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onBucketEmpty(event: PlayerBucketEmptyEvent, priority: EventPriority) {
            val rebarItem = event.itemStack?.let { RebarItem.fromStack(it, BucketRebarItemHandler::class.java) }
            if (rebarItem !is RebarItem) return
            if (!event.player.canUse(rebarItem, false)) {
                event.isCancelled = true
                return
            }

            try {
                MultiHandlers.handleEvent(rebarItem, "onBucketEmpty", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }

        @UniversalHandler
        private fun onBucketFill(event: PlayerBucketFillEvent, priority: EventPriority) {
            val stack = event.player.inventory.getItem(event.hand) // TODO: find out why this doesn't just use the fucking event's item like emptied
            val rebarItem = RebarItem.fromStack(stack, BucketRebarItemHandler::class.java)
            if (rebarItem !is RebarItem) return
            if (!event.player.canUse(rebarItem, false)) {
                event.isCancelled = true
                return
            }

            try {
                MultiHandlers.handleEvent(rebarItem, "onBucketFill", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }
    }
}