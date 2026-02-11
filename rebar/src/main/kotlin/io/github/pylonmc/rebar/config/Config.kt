package io.github.pylonmc.rebar.config

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File
import java.nio.file.Path

/**
 * Wraps a config file and provides useful facilities for writing/reading it.
 *
 * If the file changes on disk, you will need to create a new Config object to
 * get the latest version of the file.
 *
 * @property internalConfig The [YamlConfiguration] that this object wraps.
 */
class Config(
    val file: File,
    val internalConfig: YamlConfiguration = YamlConfiguration.loadConfiguration(file)
) : ConfigSection(internalConfig) {

    constructor(path: Path) : this(path.toFile())
    constructor(plugin: Plugin, path: String) : this(File(plugin.dataFolder, path))

    /**
     * Saves the configuration to the file it was loaded from.
     */
    fun save() {
        internalConfig.save(file)
    }

    override fun modifyException(exception: Exception): Exception = when (exception) {
        is KeyNotFoundException -> KeyNotFoundException(
            exception.key,
            "Key '${exception.key}' not found in config file ${file.absolutePath}"
        )

        else -> RuntimeException(
            "An error occurred while reading config file ${file.absolutePath} (CHECK THE SUB-EXCEPTION BEFORE REPORTING)",
            exception
        )
    }
}