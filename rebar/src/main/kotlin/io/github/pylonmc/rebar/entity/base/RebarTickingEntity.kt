package io.github.pylonmc.rebar.entity.base

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.async.BukkitMainThreadDispatcher
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.entity.EntityListener
import io.github.pylonmc.rebar.entity.RebarEntity
import io.github.pylonmc.rebar.entity.RebarEntitySchema
import io.github.pylonmc.rebar.event.RebarEntityAddEvent
import io.github.pylonmc.rebar.event.RebarEntityDeathEvent
import io.github.pylonmc.rebar.event.RebarEntitySerializeEvent
import io.github.pylonmc.rebar.util.delayTicks
import io.github.pylonmc.rebar.util.rebarKey
import kotlinx.coroutines.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.jetbrains.annotations.ApiStatus
import java.util.IdentityHashMap

/**
 * Represents an entity that 'ticks' (does something at a fixed time interval).
 */
interface RebarTickingEntity {

    private val tickingData: TickingEntityData
        get() = tickingEntities.getOrPut(this) { TickingEntityData(
            RebarConfig.DEFAULT_TICK_INTERVAL,
            false,
            null
        )}

    /**
     * The interval at which the [tick] function is called. You should generally use [setTickInterval]
     * in your place constructor instead of overriding this.
     */
    val tickInterval
        get() = tickingData.tickInterval

    /**
     * Whether the [tick] function should be called asynchronously. You should generally use
     * [setAsync] in your place constructor instead of overriding this.
     */
    val isAsync
        get() = tickingData.isAsync

    /**
     * Sets how often the [tick] function should be called (in Minecraft ticks)
     */
    fun setTickInterval(tickInterval: Int) {
        tickingData.tickInterval = tickInterval
    }

    /**
     * Sets whether the [tick] function should be called asynchronously.
     *
     * WARNING: Setting an entity to tick asynchronously could have unintended consequences.
     *
     * Only set this option if you understand what 'asynchronous' means, and note that you
     * cannot interact with the world asynchronously.
     */
    fun setAsync(isAsync: Boolean) {
        tickingData.isAsync = isAsync
    }

    /**
     * The function that should be called periodically.
     */
    fun tick()

    @ApiStatus.Internal
    companion object : Listener {

        data class TickingEntityData(
            var tickInterval: Int,
            var isAsync: Boolean,
            var job: Job?,
        )

        private val tickingEntityKey = rebarKey("ticking_entity_data")

        private val tickingEntities = IdentityHashMap<RebarTickingEntity, TickingEntityData>()

        @EventHandler
        private fun onAdded(event: RebarEntityAddEvent) {
            val entity = event.rebarEntity
            if (entity is RebarTickingEntity) {
                startTicker(entity)
            }
        }

        @EventHandler
        private fun onUnload(event: RebarEntitySerializeEvent) {
            val entity = event.rebarEntity
            if (entity is RebarTickingEntity) {
                tickingEntities.remove(entity)?.job?.cancel()
            }
        }

        @EventHandler
        private fun onDeath(event: RebarEntityDeathEvent) {
            val entity = event.rebarEntity
            if (entity is RebarTickingEntity) {
                tickingEntities.remove(entity)?.job?.cancel()
            }
        }

        /**
         * Returns true if the entity is still ticking, or false if the entity does
         * not exist, is not a ticking entity, or has errored and been unloaded.
         */
        @JvmStatic
        @ApiStatus.Internal
        fun isTicking(entity: RebarEntity<*>?): Boolean {
            return entity is RebarTickingEntity && tickingEntities[entity]?.job?.isActive == true
        }

        @JvmSynthetic
        internal fun stopTicking(entity: RebarTickingEntity) {
            tickingEntities[entity]?.job?.cancel()
        }

        private val dispatchers = mutableMapOf<RebarEntitySchema, CoroutineDispatcher>()

        private fun startTicker(tickingEntity: RebarTickingEntity) {
            val dispatcher = dispatchers.getOrPut((tickingEntity as RebarEntity<*>).schema) {
                if (tickingEntity.isAsync) Dispatchers.Default
                else BukkitMainThreadDispatcher(Rebar, 1)
            }
            tickingEntities[tickingEntity]?.job = Rebar.scope.launch(dispatcher) {
                while (true) {
                    delayTicks(tickingEntity.tickInterval.toLong())
                    try {
                        tickingEntity.tick()
                    } catch (e: Exception) {
                        withContext(Rebar.mainThreadDispatcher) {
                            EntityListener.logEventHandleErrTicking(e, tickingEntity as RebarEntity<*>)
                        }
                    }
                }
            }
        }
    }
}