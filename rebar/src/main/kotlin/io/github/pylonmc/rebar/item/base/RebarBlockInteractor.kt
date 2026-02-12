package io.github.pylonmc.rebar.item.base

import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import io.github.pylonmc.rebar.item.research.Research.Companion.canUse
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerInteractEvent

interface RebarBlockInteractor : RebarCooldownable {
    /**
     * May be fired twice (once for each hand), and is fired for both left and right clicks.
     */
    fun onUsedToClickBlock(event: PlayerInteractEvent, priority: EventPriority)

    companion object : MultiListener {
        @UniversalHandler
        private fun onUsedToClickBlock(event: PlayerInteractEvent, priority: EventPriority) {
            if (!event.hasBlock()) return
            val rebarItem = event.item?.let { RebarItem.fromStack(it) } ?: return
            if (rebarItem !is RebarBlockInteractor) return
            if (!event.player.canUse(rebarItem, false)) {
                event.isCancelled = true
                return
            } else if (rebarItem.respectCooldown && event.player.getCooldown(rebarItem.stack) > 0) {
                event.isCancelled = true
                return
            }

            try {
                MultiHandler.handleEvent(rebarItem, RebarBlockInteractor::class.java, "onUsedToClickBlock", event, priority)
            } catch (e: Exception) {
                RebarItemListener.logEventHandleErr(event, e, rebarItem)
            }
        }
    }
}