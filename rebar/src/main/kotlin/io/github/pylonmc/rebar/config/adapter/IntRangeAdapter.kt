package io.github.pylonmc.rebar.config.adapter

import org.bukkit.configuration.ConfigurationSection

object IntRangeAdapter : ConfigAdapter<IntRange> {

    override val type = IntRange::class.java

    override fun convert(key: String?, value: Any): IntRange {
        val min: Int
        val max: Int
        when (value) {
            is List<*> -> {
                if (value.isEmpty() || value.size > 2) {
                    throw IllegalArgumentException("Expected a list of 1 or 2 integers, but got: $value")
                }
                min = ConfigAdapter.INTEGER.convert(key, value[0]!!)
                max = ConfigAdapter.INTEGER.convert(key, value.getOrElse(1) { value[0] }!!)
            }

            is Map<*, *>, is ConfigurationSection -> {
                val section = ConfigAdapter.CONFIG_SECTION.convert(key, value)
                min = section.getOrThrow("min", ConfigAdapter.INTEGER)
                max = section.getOrThrow("max", ConfigAdapter.INTEGER)
            }

            else -> {
                val intValue = ConfigAdapter.INTEGER.convert(key, value)
                min = intValue
                max = intValue
            }
        }
        return min..max
    }
}