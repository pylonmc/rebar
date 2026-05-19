package io.github.pylonmc.rebar.config.adapter

import org.apache.commons.lang3.reflect.TypeUtils
import org.bukkit.configuration.ConfigurationSection
import java.lang.reflect.Type

class MapConfigAdapter<K, V>(
    private val keyAdapter: ConfigAdapter<K>,
    private val valueAdapter: ConfigAdapter<V>
) : ConfigAdapter<Map<K, V>> {

    override val type: Type = TypeUtils.parameterize(Map::class.java, keyAdapter.type, valueAdapter.type)

    override fun convert(key: String?, value: Any): Map<K, V> {
        if (value is ConfigurationSection) {
            return convert(key, value.getValues(false))
        }
        return buildMap {
            for ((k, v) in (value as Map<*, *>)) {
                @Suppress("UNCHECKED_CAST")
                put(keyAdapter.convert(key, k!!), v?.let { valueAdapter.convert(key, it) } as V)
            }
        }
    }

    companion object {
        @JvmStatic
        fun <K, V> from(keyAdapter: ConfigAdapter<K>, valueAdapter: ConfigAdapter<V>): ConfigAdapter<Map<K, V>> {
            return MapConfigAdapter(keyAdapter, valueAdapter)
        }

        @JvmField
        val STRING_TO_ANY = from(ConfigAdapter.STRING, ConfigAdapter.ANY)
    }
}