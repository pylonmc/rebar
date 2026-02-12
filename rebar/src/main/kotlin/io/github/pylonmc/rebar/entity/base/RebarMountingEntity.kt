package io.github.pylonmc.rebar.entity.base

import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDismountEvent
import org.bukkit.event.entity.EntityMountEvent

interface RebarMountingEntity {
    fun onMount(event: EntityMountEvent, priority: EventPriority) {}
    fun onDismount(event: EntityDismountEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onMount(event: EntityMountEvent, priority: EventPriority) {
            val mounter = EntityStorage.get(event.entity)
            if (mounter is RebarMountingEntity) {
                try {
                    MultiHandler.handleEvent(mounter, RebarMountingEntity::class.java, "onMount", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, mounter)
                }
            }
        }

        @UniversalHandler
        private fun onDismount(event: EntityDismountEvent, priority: EventPriority) {
            val dismounter = EntityStorage.get(event.entity)
            if (dismounter is RebarMountingEntity) {
                try {
                    MultiHandler.handleEvent(dismounter, RebarMountingEntity::class.java, "onDismount", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, dismounter)
                }
            }
        }
    }
}