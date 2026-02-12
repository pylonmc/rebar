package io.github.pylonmc.rebar.item.base

import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import io.github.pylonmc.rebar.item.research.Research.Companion.canUse
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerBucketFillEvent

interface RebarBucket {
    /**
     * Called when the bucket is emptied.
     */
    fun onBucketEmptied(event: PlayerBucketEmptyEvent, priority: EventPriority) {}

    /**
     * Called when the bucket is filled.
     */
    fun onBucketFilled(event: PlayerBucketFillEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onBucketEmptied(event: PlayerBucketEmptyEvent, priority: EventPriority) {
            val rebarItem = event.itemStack?.let { RebarItem.fromStack(it) }
            if (rebarItem !is RebarBucket) return
            if (!event.player.canUse(rebarItem, false)) {
                event.isCancelled = true
                return
            }

            try {
                MultiHandler.handleEvent(rebarItem, RebarBucket::class.java, "onBucketEmptied", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }

        @UniversalHandler
        private fun onBucketFilled(event: PlayerBucketFillEvent, priority: EventPriority) {
            val stack = event.player.inventory.getItem(event.hand) // TODO: find out why this doesn't just use the fucking event's item like emptied
            val rebarItem = RebarItem.fromStack(stack)
            if (rebarItem !is RebarBucket) return
            if (!event.player.canUse(rebarItem, false)) {
                event.isCancelled = true
                return
            }

            try {
                MultiHandler.handleEvent(rebarItem, RebarBucket::class.java, "onBucketFilled", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }
    }
}