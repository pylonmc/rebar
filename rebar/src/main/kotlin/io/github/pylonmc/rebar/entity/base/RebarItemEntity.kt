package io.github.pylonmc.rebar.entity.base

import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.ItemDespawnEvent
import org.bukkit.event.entity.ItemMergeEvent

interface RebarItemEntity {
    fun onDespawn(event: ItemDespawnEvent, priority: EventPriority) {}
    fun onMerge(event: ItemMergeEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onDespawn(event: ItemDespawnEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarItemEntity) {
                try {
                    MultiHandler.handleEvent(rebarEntity, "onDespawn", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onMerge(event: ItemMergeEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarItemEntity) {
                try {
                    MultiHandler.handleEvent(rebarEntity, "onMerge", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}