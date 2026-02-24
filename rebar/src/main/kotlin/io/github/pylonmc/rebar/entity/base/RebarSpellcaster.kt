package io.github.pylonmc.rebar.entity.base

import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntitySpellCastEvent

interface RebarSpellcaster {
    fun onCastSpell(event: EntitySpellCastEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onSpellCast(event: EntitySpellCastEvent, priority: EventPriority) {
            val rebarEntity = EntityStorage.get(event.entity)
            if (rebarEntity is RebarSpellcaster) {
                try {
                    MultiHandlers.handleEvent(rebarEntity, "onCastSpell", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, rebarEntity)
                }
            }
        }
    }
}