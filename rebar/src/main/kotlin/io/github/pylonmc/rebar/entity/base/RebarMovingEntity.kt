package io.github.pylonmc.rebar.entity.base

import com.destroystokyo.paper.event.entity.EntityJumpEvent
import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.papermc.paper.event.entity.EntityKnockbackEvent
import io.papermc.paper.event.entity.EntityMoveEvent
import io.papermc.paper.event.entity.EntityToggleSitEvent
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.event.entity.EntityToggleSwimEvent
import org.jetbrains.annotations.ApiStatus

interface RebarMovingEntity {
    fun onMove(event: EntityMoveEvent, priority: EventPriority) {}
    fun onJump(event: EntityJumpEvent, priority: EventPriority) {}
    fun onKnockback(event: EntityKnockbackEvent, priority: EventPriority) {}
    fun onToggleSwim(event: EntityToggleSwimEvent, priority: EventPriority) {}
    fun onToggleGlide(event: EntityToggleGlideEvent, priority: EventPriority) {}
    fun onToggleSit(event: EntityToggleSitEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        /**
         * Because the EntityMoveEvent is often called hundreds if not thousands of times a tick, the overhead of checking
         * if an entity is a RebarEntity stacks up. Every time the event is called, we process the event 6
         * times (1 for each priority). To avoid this we have a variable that stores the last EntityMoveEvent that was not a RebarEntity
         * and if the current event is the same as that one, we ignore it.
         *
         * EntityMoveEvent is a sync event so there should be no issues with concurrency.
         *
         * Ideally this is a temporary measure, we should try and find a better way to handle this (maybe optimize entity lookup if possible)
         * and abstract it so other high frequency events can be handled to avoid the same issue.
         */
        private var ignoredMoveEvent: EntityMoveEvent? = null

        @UniversalHandler
        private fun onMove(event: EntityMoveEvent, priority: EventPriority) {
            if (event == ignoredMoveEvent) {
                if (priority == EventPriority.MONITOR) {
                    ignoredMoveEvent = null
                }
                return
            }

            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarMovingEntity) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onMove", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            } else {
                ignoredMoveEvent = event
            }
        }

        @UniversalHandler
        private fun onJump(event: EntityJumpEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarMovingEntity) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onJump", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onKnockback(event: EntityKnockbackEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarMovingEntity) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onKnockback", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onToggleSwim(event: EntityToggleSwimEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarMovingEntity) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onToggleSwim", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onToggleGlide(event: EntityToggleGlideEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarMovingEntity) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onToggleGlide", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onToggleSit(event: EntityToggleSitEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarMovingEntity) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onToggleSit", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}