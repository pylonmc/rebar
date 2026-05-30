package io.github.pylonmc.rebar.config

import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import io.github.pylonmc.rebar.util.getAddon
import io.github.pylonmc.rebar.util.mergeResource
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.nio.file.Path
import java.util.WeakHashMap

/**
 * A wrapper around [ConfigurationSection] providing useful utilities for reading/writing.
 *
 * All get calls are cached, so performance is generally a non-issue here.
 *
 * @see ConfigAdapter
 * @see ConfigurationSection
 */
open class ConfigSection private constructor(val name: String?, val internalSection: ConfigurationSection) {

    private val cache: MutableMap<String, Any?> = WeakHashMap()
    private val sectionCache: MutableMap<String, ConfigSection> = WeakHashMap()

    /**
     * Saves the configuration to the specified file, overwriting the contents of the file.
     *
     * Creates the file if it does not exist.
     */
    fun save(file: File) {
        val config = YamlConfiguration()
        for (key in internalSection.getKeys(false)) {
            config.set(key, internalSection.get(key))
        }
        config.save(file)
    }

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
        val configSection = ConfigSection(name, newConfig)
        sectionCache[key] = configSection
        return configSection
    }

    fun getSectionOrThrow(key: String): ConfigSection =
        getSection(key) ?: throw KeyNotFoundException(getKeyPath(key))

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
        fun getClass(type: Type): Class<*> = when (type) {
            is Class<*> -> type
            is ParameterizedType -> getClass(type.rawType)
            else -> throw IllegalArgumentException("Unsupported type: $type")
        }

        val cached = cache[key]
        if (cached != null) {
            @Suppress("UNCHECKED_CAST")
            val clazz = getClass(adapter.type) as Class<T>
            try {
                return clazz.cast(cached)
            } catch (e: Exception) {
                throw IllegalArgumentException(
                    "You have attempted to access ${getKeyPath(key)} with two different config adapters. Ensure you are using the same config adapater every time you read this value.",
                    e
                )
            }
        }

        val rawValue = internalSection.get(key) ?: throw KeyNotFoundException(getKeyPath(key))
        val value = try {
            adapter.convert(rawValue)
        } catch (e: KeyNotFoundException) {
            val exception = KeyNotFoundException("$key.${e.keyPath.removePrefix("$key.")}")
            exception.stackTrace = e.stackTrace
            throw exception
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "Failed to convert value '$rawValue' to type ${adapter.type} at ${getKeyPath(key)}",
                e
            )
        }

        cache[key] = value

        @Suppress("UNCHECKED_CAST")
        val clazz = getClass(adapter.type) as Class<T>
        return clazz.cast(value)
    }

    fun <T> set(key: String, value: T) {
        internalSection.set(key, value)
        cache.remove(key)
    }

    fun createSection(key: String): ConfigSection =
        ConfigSection(name, internalSection.createSection(key)).also { sectionCache[key] = it }

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

    private fun getKeyPath(key: String): String {
        var path = ""
        if (name != null) {
            path += "$name:"
        }
        if (!internalSection.currentPath.isNullOrEmpty()) {
            path += "${internalSection.currentPath}."
        }
        path += key
        return path
    }

    /**
     * Thrown when a key is not found.
     */
    class KeyNotFoundException(
        val keyPath: String,
        message: String = "Config key not found: $keyPath"
    ) : RuntimeException(message)

    companion object {

        /**
         * Loads a [ConfigSection] from a [ConfigurationSection].
         *
         * [name] is optional. It is used to identify the returned [ConfigSection] in errors.
         */
        @JvmStatic
        fun from(name: String?, configSection: ConfigurationSection)
                = ConfigSection(name, configSection)

        /**
         * Returns null if the file does not exist
         */
        @JvmStatic
        fun from(file: File): ConfigSection? {
            if (!file.exists()) {
                return null
            }
            return from(file.absolutePath, YamlConfiguration.loadConfiguration(file))
        }

        @JvmStatic
        fun fromOrThrow(file: File)
                = from(file) ?: throw IllegalArgumentException("${file.absolutePath} does not exist")

        /**
         * Returns null if the file does not exist
         */
        @JvmStatic
        fun from(path: Path)
                = from(path.toFile())

        @JvmStatic
        fun fromOrThrow(path: Path)
                = from(path) ?: throw IllegalArgumentException("${path.toFile().absolutePath} does not exist")

        /**
         * Loads a config from [plugin]'s data folder, e.g. plugins/pylon/ for Pylon.
         *
         * [path] is relative to the data folder.
         *
         * Returns null if the file does not exist
         */
        @JvmStatic
        fun fromDataFolder(plugin: Plugin, path: String)
                = from(File(plugin.dataFolder, path))

        /**
         * Loads a config from [plugin]'s data folder, e.g. plugins/pylon/ for Pylon.
         *
         * [path] is relative to the data folder.
         **/
        @JvmStatic
        fun fromDataFolderOrThrow(plugin: Plugin, path: String)
                = fromDataFolder(plugin, path)
            ?: throw IllegalArgumentException("Failed to load config from ${File(plugin.dataFolder, path).absolutePath} because it does not exist")

        /**
         * Loads a config from the resource embedded in your plugin, meaning
         * anything inside your `resources` folder.
         *
         * Returns null if the file does not exist
         */
        @JvmStatic
        fun fromResource(plugin: Plugin, path: String): ConfigSection? {
            val resource = plugin.getResource(path) ?: return null
            val config = YamlConfiguration()
            try {
                // We cannot use YamlConfiguration.loadConfiguration because it catches all errors internally...
                config.load(resource.reader())
            } catch (e: Exception) {
                throw RuntimeException("Failed to load config from resources/$path because it is not valid YAML", e)
            }
            return from("resources/$path", config)
        }

        /**
         * Loads a config from the resource embedded in your plugin, meaning
         * anything inside your `resources` folder.
         */
        @JvmStatic
        fun fromResourceOrThrow(plugin: Plugin, path: String)
                = fromResource(plugin, path) ?: throw IllegalArgumentException("Failed to load config from resources/$path because it does not exist")

        /**
         * Retrieves the settings for the given [key] from the Rebar settings folder.
         *
         * If an exposed config for the key does not already exist, your addon's resources/settings
         * folder will be checked for the settings file. If found, this file will be copied to the
         * Rebar settings folder and returned. If not, an error will be thrown.
         */
        @JvmStatic
        fun fromSettings(key: NamespacedKey)
            = mergeResource(getAddon(key), "settings/${key.key}.yml", "settings/${key.namespace}/${key.key}.yml")

        /**
         * Copies a resource (i.e. a file) from your addon's `resources` folder to its data folder.
         *
         * For example, calling `copyResource(Pylon.getInstance(), "some_folder/some_config.yml)` would
         * copy `resources/some_folder/some_config.yml` to `plugins/Pylon/some_folder/some_config.yml`
         *
         * If this is called and the file already exists in the data folder, then the file in `resources`
         * will be merged on top of this one, meaning any missing keys will be copied to the data
         * folder's file.
         */
        @JvmStatic
        fun copyResource(plugin: Plugin, path: String): ConfigSection {
            val config = fromResource(plugin, path)
                ?: throw IllegalArgumentException("Failed to load config from ${File(plugin.dataFolder, path).absolutePath} because it does not exist")
            config.save(File(plugin.dataFolder, path))
            return fromDataFolder(plugin, path)!!
        }
    }
}
