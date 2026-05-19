package io.github.pylonmc.rebar.config.adapter

import org.apache.commons.lang3.reflect.TypeUtils
import org.bukkit.configuration.ConfigurationSection
import java.lang.reflect.Type

class SetConfigAdapter<E>(private val elementAdapter: ConfigAdapter<E>) : ConfigAdapter<Set<E>> {

    override val type: Type = TypeUtils.parameterize(Set::class.java, elementAdapter.type)

    override fun convert(key: String?, value: Any): Set<E> {
        val list = when (value) {
            is List<*> -> value
            is ConfigurationSection, is Map<*, *> -> MapConfigAdapter.STRING_TO_ANY.convert(key, value).toList()
            else -> throw IllegalArgumentException("Expected a list or section, but got: ${value::class.java.name}")
        }
        return list.mapTo(mutableSetOf()) {
            @Suppress("UNCHECKED_CAST")
            it?.let { elementAdapter.convert(key, it) } as E
        }
    }

    companion object {
        @JvmStatic
        fun <E> from(elementAdapter: ConfigAdapter<E>): ConfigAdapter<Set<E>> {
            return SetConfigAdapter(elementAdapter)
        }
    }
}