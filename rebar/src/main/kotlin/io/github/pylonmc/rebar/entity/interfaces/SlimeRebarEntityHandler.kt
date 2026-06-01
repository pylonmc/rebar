package io.github.pylonmc.rebar.entity.interfaces

import com.destroystokyo.paper.event.entity.*
import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.SlimeSplitEvent
import org.jetbrains.annotations.ApiStatus

interface SlimeRebarEntityHandler {
    fun onSlimeSwim(event: SlimeSwimEvent, priority: EventPriority) {}
    fun onSlimeSplit(event: SlimeSplitEvent, priority: EventPriority) {}
    fun onSlimeWander(event: SlimeWanderEvent, priority: EventPriority) {}
    fun onSlimePathfind(event: SlimePathfindEvent, priority: EventPriority) {}
    fun onSlimeChangeDirection(event: SlimeChangeDirectionEvent, priority: EventPriority) {}
    fun onSlimeTarget(event: SlimeTargetLivingEntityEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onSlimeSwim(event: SlimeSwimEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is SlimeRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onSlimeSwim", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onSlimeSplit(event: SlimeSplitEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is SlimeRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onSlimeSplit", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onSlimeWander(event: SlimeWanderEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is SlimeRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onSlimeWander", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onSlimePathfind(event: SlimePathfindEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is SlimeRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onSlimePathfind", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onSlimeChangeDirection(event: SlimeChangeDirectionEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is SlimeRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onSlimeChangeDirection", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onSlimeTarget(event: SlimeTargetLivingEntityEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is SlimeRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onSlimeTarget", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}