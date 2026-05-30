package io.github.pylonmc.rebar.entity.interfaces

import com.destroystokyo.paper.event.entity.WitchConsumePotionEvent
import com.destroystokyo.paper.event.entity.WitchReadyPotionEvent
import com.destroystokyo.paper.event.entity.WitchThrowPotionEvent
import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.jetbrains.annotations.ApiStatus

interface WitchRebarEntityHandler {
    fun onWitchConsumePotion(event: WitchConsumePotionEvent, priority: EventPriority) {}
    fun onWitchReadyPotion(event: WitchReadyPotionEvent, priority: EventPriority) {}
    fun onWitchThrowPotion(event: WitchThrowPotionEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onWitchConsumePotion(event: WitchConsumePotionEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is WitchRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onWitchConsumePotion", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onWitchReadyPotion(event: WitchReadyPotionEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is WitchRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onWitchReadyPotion", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onWitchThrowPotion(event: WitchThrowPotionEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is WitchRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onWitchThrowPotion", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}