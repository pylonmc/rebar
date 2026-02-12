package io.github.pylonmc.rebar.entity.base

import com.destroystokyo.paper.event.entity.WitchConsumePotionEvent
import com.destroystokyo.paper.event.entity.WitchReadyPotionEvent
import com.destroystokyo.paper.event.entity.WitchThrowPotionEvent
import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority

interface RebarWitch {
    fun onConsumePotion(event: WitchConsumePotionEvent, priority: EventPriority) {}
    fun onReadyPotion(event: WitchReadyPotionEvent, priority: EventPriority) {}
    fun onThrowPotion(event: WitchThrowPotionEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onConsumePotion(event: WitchConsumePotionEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarWitch) {
                try {
                    MultiHandler.handleEvent(rebarEntity, "onConsumePotion", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onReadyPotion(event: WitchReadyPotionEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarWitch) {
                try {
                    MultiHandler.handleEvent(rebarEntity, "onReadyPotion", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }

        @UniversalHandler
        private fun onThrowPotion(event: WitchThrowPotionEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarWitch) {
                try {
                    MultiHandler.handleEvent(rebarEntity, "onThrowPotion", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}