package io.github.pylonmc.rebar.entity.base

import com.destroystokyo.paper.event.entity.ExperienceOrbMergeEvent
import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority

interface RebarExperienceOrb {
    fun onMergeOrb(event: ExperienceOrbMergeEvent, priority: EventPriority) {}
    fun onAbsorbedByOrb(event: ExperienceOrbMergeEvent, priority: EventPriority) {}

    companion object : MultiListener {
        @UniversalHandler
        private fun onMergeOrb(event: ExperienceOrbMergeEvent, priority: EventPriority) {
            val source = EntityStorage.get(event.mergeSource)
            if (source is RebarExperienceOrb) {
                try {
                    MultiHandlers.handleEvent(source, "onMergeOrb", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, source)
                }
            }
        }

        @UniversalHandler
        private fun onAbsorbedByOrb(event: ExperienceOrbMergeEvent, priority: EventPriority) {
            val target = EntityStorage.get(event.mergeTarget)
            if (target is RebarExperienceOrb) {
                try {
                    MultiHandlers.handleEvent(target, "onAbsorbedByOrb", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, target)
                }
            }
        }
    }
}