package io.github.pylonmc.rebar.world

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.config.RebarConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.WorldUnloadEvent
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import java.util.*
import java.util.function.Supplier
import kotlin.time.Duration.Companion.seconds

/**
 * Provides a wrapper around a [World]'s [PersistentDataContainer] that caches values in memory and periodically saves
 * them back to the world to reduce disk I/O. The data is automatically saved when the world is unloaded.
 */
class WorldStorage private constructor(val worldId: UUID) {

    val world: World
        get() = Bukkit.getWorld(worldId)!!

    private val data = mutableMapOf<NamespacedKey, Pair<*, PersistentDataType<*, *>>>()
    private val dirtyData = mutableMapOf<NamespacedKey, Job>()

    operator fun <T : Any> get(key: NamespacedKey, type: PersistentDataType<*, T>): T? {
        val data = data.getOrPut(key) { world.persistentDataContainer.get(key, type) to type }.first
        return type.complexType.cast(data)
    }

    fun <T : Any> getOrThrow(key: NamespacedKey, type: PersistentDataType<*, T>): T {
        return get(key, type)
            ?: throw NoSuchElementException("No value found in world storage for key $key of type ${type.complexType.name}")
    }

    fun <T : Any> getOrDefault(key: NamespacedKey, type: PersistentDataType<*, T>, default: T): T {
        return get(key, type) ?: default
    }

    fun <T : Any> getOrPut(key: NamespacedKey, type: PersistentDataType<*, T>, defaultValue: Supplier<T>): T {
        var value = get(key, type)
        if (value != null) {
            return value
        }
        value = defaultValue.get()
        set(key, type, value)
        return value
    }

    operator fun <T : Any> set(key: NamespacedKey, type: PersistentDataType<*, T>, value: T) {
        data[key] = value to type
        dirtyData.remove(key)?.cancel()
        dirtyData[key] = Rebar.scope.launch {
            delay(RebarConfig.WORLD_DATA_AUTOSAVE_INTERVAL_SECONDS.seconds)
            saveData(key)
        }
    }

    fun remove(key: NamespacedKey) {
        data.remove(key)
        dirtyData.remove(key)?.cancel()
        dirtyData[key] = Rebar.scope.launch {
            delay(RebarConfig.WORLD_DATA_AUTOSAVE_INTERVAL_SECONDS.seconds)
            saveData(key)
        }
    }

    /**
     * Preloads a value from the world's persistent data container into memory. This can be used to reduce lag spikes when
     * accessing data for the first time by loading it during a less performance-sensitive time, such as during world initialization.
     */
    fun preload(key: NamespacedKey, type: PersistentDataType<*, *>) {
        get(key, type)
    }

    private fun saveData(key: NamespacedKey) {
        val pdc = world.persistentDataContainer
        val unsavedData = data[key]
        if (unsavedData == null) {
            pdc.remove(key)
        } else {
            @Suppress("UNCHECKED_CAST")
            pdc.set(key, unsavedData.second as PersistentDataType<*, Any>, unsavedData.first as Any)
        }
        dirtyData.remove(key)?.cancel()
    }

    fun saveAll() {
        for (key in dirtyData.keys.toList()) {
            saveData(key)
        }
    }

    companion object : Listener {
        private val storages = mutableMapOf<UUID, WorldStorage>()

        fun getStorage(world: World): WorldStorage {
            return storages.getOrPut(world.uid) { WorldStorage(world.uid) }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        private fun onWorldUnload(event: WorldUnloadEvent) {
            storages.remove(event.world.uid)?.saveAll()
        }
    }
}