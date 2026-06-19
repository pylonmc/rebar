package io.github.pylonmc.rebar.config.adapter

import org.apache.commons.lang3.reflect.TypeUtils
import org.bukkit.configuration.ConfigurationSection
import java.lang.reflect.Type

class MapConfigAdapter<K, V>(
    private val keyAdapter: ConfigAdapter<K>,
    private val valueAdapter: ConfigAdapter<V>
) : ConfigAdapter<Map<K, V>> {

    override val type: Type = TypeUtils.parameterize(Map::class.java, keyAdapter.type, valueAdapter.type)

    override fun convert(value: Any): Map<K, V> {
        when (value) {
            is List<*> -> {
                val map = mutableMapOf<K, V>()
                for (maybeSection in value) {
                    val section = STRING_TO_ANY.convert(maybeSection!!)
                    val key = keyAdapter.convert(
                        section["key"]
                            ?: error("An entry in the map is missing 'key'")
                    )
                    check(key !in map) { "Map contains duplicate value $key" }
                    val value = valueAdapter.convert(
                        section["value"]
                            ?: error("An entry in the map is missing 'value'")
                    )
                    map[key] = value
                }
                return map
            }

            is ConfigurationSection -> {
                return convert(value.getValues(false))
            }

            else -> {
                val map = mutableMapOf<K, V>()
                    for ((k, v) in (value as Map<*, *>)) {
                        val key = keyAdapter.convert(k!!)
                        check(key !in map) { "Map contains duplicate value $key" }
                        @Suppress("UNCHECKED_CAST")
                        map[key] = v?.let(valueAdapter::convert) as V
                    }
                return map
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