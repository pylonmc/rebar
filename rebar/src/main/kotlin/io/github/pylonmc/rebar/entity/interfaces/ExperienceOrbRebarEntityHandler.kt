package io.github.pylonmc.rebar.entity.interfaces

import com.destroystokyo.paper.event.entity.ExperienceOrbMergeEvent
import io.github.pylonmc.rebar.entity.EntityListener.logEventHandleErr
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.jetbrains.annotations.ApiStatus

interface ExperienceOrbRebarEntityHandler {
    fun onAbsorbExperienceOrb(event: ExperienceOrbMergeEvent, priority: EventPriority) {}
    fun onAbsorbedByExperienceOrb(event: ExperienceOrbMergeEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onExperienceOrbMerge(event: ExperienceOrbMergeEvent, priority: EventPriority) {
            val target = EntityStorage.get(event.mergeTarget)
            if (target is ExperienceOrbRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(target, "onAbsorbExperienceOrb", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, target)
                }
            }

            val source = EntityStorage.get(event.mergeSource)
            if (source is ExperienceOrbRebarEntityHandler) {
                try {
                    MultiHandlers.handleEvent(source, "onAbsorbedByExperienceOrb", event, priority)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, source)
                }
            }
        }
    }
}