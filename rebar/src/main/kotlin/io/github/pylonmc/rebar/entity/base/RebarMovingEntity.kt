package io.github.pylonmc.rebar.entity.base

import com.destroystokyo.paper.event.entity.EntityJumpEvent
import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.papermc.paper.event.entity.EntityKnockbackEvent
import io.papermc.paper.event.entity.EntityMoveEvent
import io.papermc.paper.event.entity.EntityToggleSitEvent
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.event.entity.EntityToggleSwimEvent

interface RebarMovingEntity {
    fun onMove(event: EntityMoveEvent, priority: EventPriority) {}
    fun onJump(event: EntityJumpEvent, priority: EventPriority) {}
    fun onKnockback(event: EntityKnockbackEvent, priority: EventPriority) {}
    fun onToggleSwim(event: EntityToggleSwimEvent, priority: EventPriority) {}
    fun onToggleGlide(event: EntityToggleGlideEvent, priority: EventPriority) {}
    fun onToggleSit(event: EntityToggleSitEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onMove(event: EntityMoveEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarMovingEntity) {
                try {
                    MultiHandler.handleEvent(rebarEntity, "onMove", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onJump(event: EntityJumpEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarMovingEntity) {
                try {
                    MultiHandler.handleEvent(rebarEntity, "onJump", event, priority)
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
                    MultiHandler.handleEvent(rebarEntity, "onKnockback", event, priority)
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
                    MultiHandler.handleEvent(rebarEntity, "onToggleSwim", event, priority)
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
                    MultiHandler.handleEvent(rebarEntity, "onToggleGlide", event, priority)
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
                    MultiHandler.handleEvent(rebarEntity, "onToggleSit", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}