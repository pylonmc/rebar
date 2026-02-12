package io.github.pylonmc.rebar.entity.base

import com.destroystokyo.paper.event.entity.*
import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.SlimeSplitEvent

interface RebarSlime {
    fun onSwim(event: SlimeSwimEvent, priority: EventPriority) {}
    fun onSplit(event: SlimeSplitEvent, priority: EventPriority) {}
    fun onWander(event: SlimeWanderEvent, priority: EventPriority) {}
    fun onPathfind(event: SlimePathfindEvent, priority: EventPriority) {}
    fun onChangeDirection(event: SlimeChangeDirectionEvent, priority: EventPriority) {}
    fun onTarget(event: SlimeTargetLivingEntityEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onSlimeSwim(event: SlimeSwimEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarSlime) {
                try {
                    MultiHandler.handleEvent(rebarEntity, RebarSlime::class.java, "onSwim", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onSlimeSplit(event: SlimeSplitEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarSlime) {
                try {
                    MultiHandler.handleEvent(rebarEntity, RebarSlime::class.java, "onSplit", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onSlimeWander(event: SlimeWanderEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarSlime) {
                try {
                    MultiHandler.handleEvent(rebarEntity, RebarSlime::class.java, "onWander", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onSlimePathfind(event: SlimePathfindEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarSlime) {
                try {
                    MultiHandler.handleEvent(rebarEntity, RebarSlime::class.java, "onPathfind", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onSlimeChangeDirection(event: SlimeChangeDirectionEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarSlime) {
                try {
                    MultiHandler.handleEvent(rebarEntity, RebarSlime::class.java, "onChangeDirection", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onSlimeTarget(event: SlimeTargetLivingEntityEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarSlime) {
                try {
                    MultiHandler.handleEvent(rebarEntity, RebarSlime::class.java, "onTarget", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}