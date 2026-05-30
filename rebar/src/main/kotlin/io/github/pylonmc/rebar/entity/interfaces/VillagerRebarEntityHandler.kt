package io.github.pylonmc.rebar.entity.interfaces

import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.VillagerAcquireTradeEvent
import org.bukkit.event.entity.VillagerCareerChangeEvent
import org.bukkit.event.entity.VillagerReplenishTradeEvent
import org.jetbrains.annotations.ApiStatus

interface VillagerRebarEntityHandler {
    fun onVillagerAcquireTrade(event: VillagerAcquireTradeEvent, priority: EventPriority) {}
    fun onVillagerCareerChange(event: VillagerCareerChangeEvent, priority: EventPriority) {}
    fun onVillagerReplenishTrade(event: VillagerReplenishTradeEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onVillagerAcquireTrade(event: VillagerAcquireTradeEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is VillagerRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onVillagerAcquireTrade", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onVillagerCareerChange(event: VillagerCareerChangeEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is VillagerRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onVillagerCareerChange", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onVillagerReplenishTrade(event: VillagerReplenishTradeEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is VillagerRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onVillagerReplenishTrade", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}