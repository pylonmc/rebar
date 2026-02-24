package io.github.pylonmc.rebar.entity

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.addon.RebarAddon
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.event.RebarEntityAddEvent
import io.github.pylonmc.rebar.event.RebarEntityDeathEvent
import io.github.pylonmc.rebar.event.RebarEntityLoadEvent
import io.github.pylonmc.rebar.event.RebarEntityUnloadEvent
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.isFromAddon
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.EntitiesLoadEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.function.Consumer
import kotlin.random.Random

/**
 * Basically [BlockStorage], but for entities. Works on all the same principles.
 *
 * @see BlockStorage
 * @see RebarEntity
 */
object EntityStorage : Listener {

    private val entities: MutableMap<UUID, RebarEntity<*>> = ConcurrentHashMap()
    private val entitiesByKey: MutableMap<NamespacedKey, MutableSet<RebarEntity<*>>> = ConcurrentHashMap()
    private val entityAutosaveTasks: MutableMap<UUID, Job> = ConcurrentHashMap()
    private val whenEntityLoadsTasks: MutableMap<UUID, MutableSet<Consumer<RebarEntity<*>>>> = ConcurrentHashMap()

    /**
     * All the loaded [RebarEntity]s
     */
    val loadedEntities: Collection<RebarEntity<*>>
        get() = entities.values

    // Access to entities, entitiesById fields must be synchronized to prevent them
    // briefly going out of sync
    private val entityLock = ReentrantReadWriteLock()

    /**
     * Returns the [RebarEntity] with this [uuid], or null if the entity does not exist or is not
     * a Rebar entity.
     */
    @JvmStatic
    fun get(uuid: UUID): RebarEntity<*>?
        = lockEntityRead { entities[uuid] }

    /**
     * Returns the [RebarEntity] corresponding to this [entity], or null if this entity is not
     * a Rebar entity.
     */
    @JvmStatic
    fun get(entity: Entity): RebarEntity<*>?
        = get(entity.uniqueId)

    /**
     * Returns the [RebarEntity] with this [uuid], or null if the entity does not exist, is not
     * a Rebar entity, or is not of type [T].
     */
    @JvmStatic
    fun <T> getAs(clazz: Class<T>, uuid: UUID): T? {
        val entity = get(uuid) ?: return null
        if (!clazz.isInstance(entity)) {
            return null
        }
        return clazz.cast(entity)
    }

    /**
     * Returns the [RebarEntity] corresponding to this [entity], or if this entity is not
     * a Rebar entity, or is not of type [T].
     */
    @JvmStatic
    fun <T> getAs(clazz: Class<T>, entity: Entity): T?
        = getAs(clazz, entity.uniqueId)

    /**
     * Returns the [RebarEntity] with this [uuid], or null if the entity does not exist, is not
     * a Rebar entity, or is not of type [T].
     */
    inline fun <reified T> getAs(uuid: UUID): T?
        = getAs(T::class.java, uuid)

    /**
     * Returns the [RebarEntity] corresponding to this [entity], or if this entity is not
     * a Rebar entity, or is not of type [T].
     */
    inline fun <reified T> getAs(entity: Entity): T?
        = getAs(T::class.java, entity)

    /**
     * Returns all the Rebar entities associated with this [key].
     */
    @JvmStatic
    fun getByKey(key: NamespacedKey): Collection<RebarEntity<*>> =
        if (key in RebarRegistry.ENTITIES) {
            lockEntityRead {
                entitiesByKey[key].orEmpty()
            }
        } else {
            emptySet()
        }

    /**
     * Schedules a task to run when the Rebar entity with id [uuid] is loaded, or runs the task immediately
     * if the entity is already loaded.
     *
     * Useful for when you don't know whether a block or one of its associated entity will be loaded first.
     */
    @JvmStatic
    fun whenEntityLoads(uuid: UUID, consumer: Consumer<RebarEntity<*>>) {
        val rebarEntity = get(uuid)
        if (rebarEntity != null) {
            consumer.accept(rebarEntity)
        } else {
            whenEntityLoadsTasks.getOrPut(uuid) { mutableSetOf() }.add {
                consumer.accept(it)
            }
        }

    }

    /**
     * Schedules a task to run when the Rebar entity with id [uuid] is loaded, or runs the task immediately
     * if the entity is already loaded.
     *
     * Useful for when you don't know whether a block or one of its associated entity will be loaded first.
     */
    @JvmStatic
    fun <T> whenEntityLoads(uuid: UUID, clazz: Class<T>, consumer: Consumer<T>) {
        val rebarEntity = getAs(clazz, uuid)
        if (rebarEntity != null) {
            consumer.accept(rebarEntity)
        } else {
            whenEntityLoadsTasks.getOrPut(uuid) { mutableSetOf() }.add {
                consumer.accept(getAs(clazz, uuid) ?: throw IllegalStateException("Entity $uuid was not of expected type ${clazz.simpleName}"))
            }
        }
    }

    /**
     * Schedules a task to run when the Rebar entity with id [uuid] is loaded, or runs the task immediately
     * if the entity is already loaded
     *
     * Useful for when you don't know whether a block or one of its associated entity will be loaded first.
     */
    @JvmStatic
    inline fun <reified T> whenEntityLoads(uuid: UUID, crossinline consumer: (T) -> Unit)
            = whenEntityLoads(uuid, T::class.java) { t -> consumer(t) }

    /**
     * Returns false if the entity is not a [RebarEntity] or does not exist.
     */
    @JvmStatic
    fun isRebarEntity(uuid: UUID): Boolean
        = get(uuid) != null

    /**
     * Returns false if the entity is not a [RebarEntity] or does not exist.
     */
    @JvmStatic
    fun isRebarEntity(entity: Entity): Boolean
        = get(entity) != null

    /**
     * Adds an entity to the storage. *This must be called for all newly spawned Rebar entities*.
     */
    @JvmStatic
    fun <E : Entity> add(entity: RebarEntity<E>): RebarEntity<E> = lockEntityWrite {
        entities[entity.uuid] = entity
        entitiesByKey.getOrPut(entity.schema.key, ::mutableSetOf).add(entity)

        // autosaving
        entityAutosaveTasks[entity.uuid] = Rebar.launch(Rebar.minecraftDispatcher) {

            // Wait a random delay before starting, this is to help smooth out lag from saving
            delay(Random.nextLong(RebarConfig.ENTITY_DATA_AUTOSAVE_INTERVAL_SECONDS * 1000))

            while (true) {
                lockEntityRead {
                    entity.write(entity.entity.persistentDataContainer)
                }
                delay(RebarConfig.ENTITY_DATA_AUTOSAVE_INTERVAL_SECONDS * 1000)
            }
        }
        RebarEntityAddEvent(entity).callEvent()
        entity
    }

    @EventHandler
    private fun onEntityLoad(event: EntitiesLoadEvent) {
        for (entity in event.entities) {
            val rebarEntity = RebarEntity.deserialize(entity) ?: continue
            add(rebarEntity)

            val tasks = whenEntityLoadsTasks[rebarEntity.uuid]
            if (tasks != null) {
                for (task in tasks) {
                    try {
                        task.accept(rebarEntity)
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                }
                whenEntityLoadsTasks.remove(rebarEntity.uuid)
            }

            RebarEntityLoadEvent(rebarEntity).callEvent()
        }
    }

    // This currently does not differentiate between unloaded and dead entities because the API
    // is broken (lol), hence the lack of an entity death listener
    @EventHandler
    private fun onEntityUnload(event: EntityRemoveFromWorldEvent) {
        val rebarEntity = get(event.entity.uniqueId) ?: return

        if (!event.entity.isDead) {
            RebarEntity.serialize(rebarEntity)
            rebarEntity.onUnload()
            RebarEntityUnloadEvent(rebarEntity).callEvent()
        } else {
            RebarEntityDeathEvent(rebarEntity, event).callEvent()
        }

        lockEntityWrite {
            entities.remove(rebarEntity.uuid)
            entitiesByKey[rebarEntity.schema.key]!!.remove(rebarEntity)
            if (entitiesByKey[rebarEntity.schema.key]!!.isEmpty()) {
                entitiesByKey.remove(rebarEntity.schema.key)
            }
            entityAutosaveTasks.remove(rebarEntity.uuid)?.cancel()
        }
    }

    @JvmSynthetic
    internal fun cleanup(addon: RebarAddon) = lockEntityWrite {
        for ((_, value) in entitiesByKey.filter { it.key.isFromAddon(addon) }) {
            for (entity in value) {
                try {
                    RebarEntity.serialize(entity)
                } catch (e: Exception) {
                    Rebar.logger.severe("Error while unloading entity ${entity.uuid} (${entity.key}")
                    e.printStackTrace()
                }
            }
        }

        entities.values.removeIf { it.schema.key.isFromAddon(addon) }
        entitiesByKey.keys.removeIf { it.isFromAddon(addon) }
    }

    @JvmSynthetic
    internal fun cleanupEverything() {
        for (entity in entities.values) {
            entity.write(entity.entity.persistentDataContainer)
        }
    }

    private inline fun <T> lockEntityRead(block: () -> T): T {
        entityLock.readLock().lock()
        try {
            return block()
        } finally {
            entityLock.readLock().unlock()
        }
    }

    private inline fun <T> lockEntityWrite(block: () -> T): T {
        entityLock.writeLock().lock()
        try {
            return block()
        } finally {
            entityLock.writeLock().unlock()
        }
    }
}