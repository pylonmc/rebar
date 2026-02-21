package io.github.pylonmc.rebar.config

import com.google.common.base.Defaults.defaultValue
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import org.bukkit.configuration.ConfigurationSection
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.WeakHashMap

/**
 * A wrapper around [ConfigurationSection] providing useful utilities for reading/writing.
 *
 * All get calls are cached, so performance is generally a non-issue here.
 *
 * @see ConfigurationSection
 */
open class ConfigSection(val internalSection: ConfigurationSection) {

    private val cache: MutableMap<String, Any?> = WeakHashMap()
    private val sectionCache: MutableMap<String, ConfigSection> = WeakHashMap()

    /**
     * Returns all the keys in the section.
     */
    val keys: Set<String>
        get() = internalSection.getKeys(false)

    /**
     * Gets all the values in the section that are themselves sections.
     * @throws NullPointerException if any top level keys do not correspond to a section
     */
    fun getSections(): List<ConfigSection> {
        val configSections: MutableList<ConfigSection> = mutableListOf()
        for (key in internalSection.getKeys(false)) {
            configSections.add(getSection(key)!!)
        }
        return configSections
    }

    fun getSection(key: String): ConfigSection? {
        val cached = sectionCache[key]
        if (cached != null) {
            return cached
        }

        val newConfig = internalSection.getConfigurationSection(key) ?: return null
        val configSection = ConfigSection(newConfig)
        sectionCache[key] = configSection
        return configSection
    }

    fun getSectionOrThrow(key: String): ConfigSection =
        getSection(key) ?: throw modifyException(KeyNotFoundException(getKeyPath(key)))

    /**
     * Returns null if the key does not exist or if the value cannot be converted to the desired type.
     */
    fun <T> get(key: String, adapter: ConfigAdapter<T>): T? {
        return runCatching { getOrThrow(key, adapter) }.getOrNull()
    }

    /**
     * Returns [defaultValue] if the key does not exist or if the value cannot be converted to the desired type.
     */
    fun <T> get(key: String, adapter: ConfigAdapter<T>, defaultValue: T): T {
        return get(key, adapter) ?: defaultValue
    }

    /**
     * Returns the computed [defaultValue] if the key does not exist or if the value cannot be converted to the desired type.
     */
    fun <T> get(key: String, adapter: ConfigAdapter<T>, defaultValue: () -> T): T {
        return get(key, adapter) ?: defaultValue.invoke()
    }

    /**
     * Throws an error if the key does not exist or if the value cannot be converted to the desired type.
     */
    fun <T> getOrThrow(key: String, adapter: ConfigAdapter<T>): T {
        val value = cache.getOrPut(key) {
            val value = internalSection.get(key) ?: throw modifyException(KeyNotFoundException(getKeyPath(key)))
            try {
                adapter.convert(value)
            } catch (e: KeyNotFoundException) {
                val exception = modifyException(KeyNotFoundException("$key.${e.key.removePrefix("$key.")}"))
                exception.stackTrace = e.stackTrace
                throw exception
            } catch (e: Exception) {
                throw modifyException(
                    IllegalArgumentException(
                        "Failed to convert value '$value' to type ${adapter.type} for key '${getKeyPath(key)}'",
                        e
                    )
                )
            }
        }

        fun getClass(type: Type): Class<*> = when (type) {
            is Class<*> -> type
            is ParameterizedType -> getClass(type.rawType)
            else -> throw modifyException(IllegalArgumentException("Unsupported type: $type"))
        }

        @Suppress("UNCHECKED_CAST")
        val clazz = getClass(adapter.type) as Class<T>
        return clazz.cast(value)
    }

    fun <T> set(key: String, value: T) {
        internalSection.set(key, value)
        cache.remove(key)
    }

    fun createSection(key: String): ConfigSection =
        ConfigSection(internalSection.createSection(key)).also { sectionCache[key] = it }

    /**
     * 'Merges' [other] with this ConfigSection by copying all of its keys into this ConfigSection.
     * If a key exists in both section, this ConfigSection's keys take priority.
     */
    fun merge(other: ConfigSection) {
        for (key in other.keys) {
            val otherSection = other.getSection(key)
            if (otherSection != null) {
                val thisSection = this.getSection(key)
                if (thisSection != null) {
                    thisSection.merge(otherSection)
                } else {
                    internalSection.set(key, otherSection.internalSection)
                }
            } else if (key !in this.keys) {
                internalSection.set(key, other.internalSection.get(key))
            }
        }
    }

    private fun getKeyPath(key: String): String =
        if (internalSection.currentPath.isNullOrEmpty()) key else "${internalSection.currentPath}.$key"

    /**
     * This exists so that [Config] can add more context to exceptions thrown by this class without having to override every method.
     * The default implementation is just `throw exception`.
     */
    protected open fun modifyException(exception: Exception): Exception = exception

    /**
     * Thrown when a key is not found.
     */
    class KeyNotFoundException(val key: String, message: String = "Config key not found: $key") : RuntimeException(message)
}