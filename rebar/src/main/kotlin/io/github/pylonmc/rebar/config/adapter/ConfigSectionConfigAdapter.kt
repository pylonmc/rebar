package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.config.ConfigSection
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemoryConfiguration

object ConfigSectionConfigAdapter : ConfigAdapter<ConfigSection> {

    override val type = ConfigSection::class.java

    override fun convert(value: Any): ConfigSection {
        val section = if (value is ConfigurationSection) {
            value
        } else {
            val memoryConfig = MemoryConfiguration()
            for ((key, value) in MapConfigAdapter.STRING_TO_ANY.convert(value)) {
                memoryConfig.set(key, value)
            }
            memoryConfig
        }
        return ConfigSection(section)
    }
}