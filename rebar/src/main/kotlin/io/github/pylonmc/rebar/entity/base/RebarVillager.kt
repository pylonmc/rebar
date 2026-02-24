package io.github.pylonmc.rebar.entity.base

import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.VillagerAcquireTradeEvent
import org.bukkit.event.entity.VillagerCareerChangeEvent
import org.bukkit.event.entity.VillagerReplenishTradeEvent

interface RebarVillager {
    fun onAcquireTrade(event: VillagerAcquireTradeEvent, priority: EventPriority) {}
    fun onCareerChange(event: VillagerCareerChangeEvent, priority: EventPriority) {}
    fun onReplenishTrade(event: VillagerReplenishTradeEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onAcquireTrade(event: VillagerAcquireTradeEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarVillager) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onAcquireTrade", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onCareerChange(event: VillagerCareerChangeEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarVillager) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onCareerChange", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onReplenishTrade(event: VillagerReplenishTradeEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarVillager) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onReplenishTrade", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}