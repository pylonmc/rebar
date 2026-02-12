package io.github.pylonmc.rebar.entity.base

import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityBreedEvent
import org.bukkit.event.entity.EntityEnterLoveModeEvent

interface RebarBreedable {
    fun onBreed(event: EntityBreedEvent, priority: EventPriority) {}
    fun onEnterLoveMode(event: EntityEnterLoveModeEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onBreed(event: EntityBreedEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarBreedable) {
                try {
                    MultiHandler.handleEvent(rebarEntity, RebarBreedable::class.java, "onBreed", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onEnterLoveMode(event: EntityEnterLoveModeEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarBreedable) {
                try {
                    MultiHandler.handleEvent(rebarEntity, RebarBreedable::class.java, "onEnterLoveMode", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}