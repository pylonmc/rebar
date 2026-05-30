package io.github.pylonmc.rebar.entity.interfaces

import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDismountEvent
import org.bukkit.event.entity.EntityMountEvent
import org.jetbrains.annotations.ApiStatus

interface MountRebarEntityHandler {
    fun onMounted(event: EntityMountEvent, priority: EventPriority) {}
    fun onDismounted(event: EntityDismountEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onMounted(event: EntityMountEvent, priority: EventPriority) {
            val mount = EntityStorage.get(event.mount)
            if (mount is MountRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(mount, "onMounted", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, mount)
                }
            }
        }

        @UniversalHandler
        private fun onDismounted(event: EntityDismountEvent, priority: EventPriority) {
            val mount = EntityStorage.get(event.dismounted)
            if (mount is MountRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(mount, "onDismounted", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, mount)
                }
            }
        }
    }
}