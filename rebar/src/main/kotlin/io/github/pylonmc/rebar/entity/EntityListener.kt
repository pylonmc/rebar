package io.github.pylonmc.rebar.entity

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.event.RebarEntityUnloadEvent
import io.github.pylonmc.rebar.event.api.MultiListener
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import java.util.*

internal object EntityListener : MultiListener {
    private val entityErrMap: MutableMap<UUID, Int> = mutableMapOf()

    @JvmSynthetic
    internal fun logEventHandleErr(event: Event, e: Exception, entity: RebarEntity<*>) {
        Rebar.logger.severe("Error when handling entity(${entity.key}, ${entity.uuid}, ${entity.entity.location}) event handler ${event.javaClass.simpleName}: ${e.localizedMessage}")
        e.printStackTrace()
        entityErrMap[entity.uuid] = entityErrMap[entity.uuid]?.plus(1) ?: 1
        if (entityErrMap[entity.uuid]!! > RebarConfig.ALLOWED_ENTITY_ERRORS) {
            entity.entity.remove()
        }
    }

    @JvmSynthetic
    internal fun logEventHandleErrTicking(e: Exception, entity: RebarEntity<*>) {
        Rebar.logger.severe("Error when handling ticking entity(${entity.key}, ${entity.uuid}, ${entity.entity.location}): ${e.localizedMessage}")
        e.printStackTrace()
        entityErrMap[entity.uuid] = entityErrMap[entity.uuid]?.plus(1) ?: 1
        if (entityErrMap[entity.uuid]!! > RebarConfig.ALLOWED_ENTITY_ERRORS) {
            entity.entity.remove()
        }
    }

    @EventHandler
    private fun onEntityUnload(event: RebarEntityUnloadEvent) {
        entityErrMap.remove(event.rebarEntity.uuid)
    }
}