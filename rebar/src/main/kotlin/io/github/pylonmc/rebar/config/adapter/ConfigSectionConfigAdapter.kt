package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.config.ConfigSection
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemoryConfiguration

object ConfigSectionConfigAdapter : ConfigAdapter<ConfigSection> {

    override val type = ConfigSection::class.java

    override fun convert(value: Any): ConfigSection {
        return if (value is ConfigurationSection) {
            ConfigSection.from(value.name, value)
        } else {
            val memoryConfig = MemoryConfiguration()
            for ((key, value) in MapConfigAdapter.STRING_TO_ANY.convert(value)) {
                memoryConfig.set(key, value)
            }
            ConfigSection.from(null, memoryConfig)
        }
    }
}