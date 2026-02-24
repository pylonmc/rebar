package io.github.pylonmc.rebar.entity.base

import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDismountEvent
import org.bukkit.event.entity.EntityMountEvent

interface RebarMountableEntity {
    fun onMount(event: EntityMountEvent, priority: EventPriority) {}
    fun onDismount(event: EntityDismountEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onMount(event: EntityMountEvent, priority: EventPriority) {
            val mount = EntityStorage.get(event.mount)
            if (mount is RebarMountableEntity) {
                try {
                    MultiHandlers.handleEvent(mount, "onMount", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, mount)
                }
            }
        }

        @UniversalHandler
        private fun onDismount(event: EntityDismountEvent, priority: EventPriority) {
            val mount = EntityStorage.get(event.dismounted)
            if (mount is RebarMountableEntity) {
                try {
                    MultiHandlers.handleEvent(mount, "onDismount", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, mount)
                }
            }
        }
    }
}