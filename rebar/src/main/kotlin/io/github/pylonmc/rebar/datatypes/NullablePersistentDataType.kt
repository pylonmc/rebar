package io.github.pylonmc.rebar.datatypes

import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType

interface NullablePersistentDataType<P, C> : PersistentDataType<P, C> {
    fun fromPrimitiveNullable(primitive: P, context: PersistentDataAdapterContext): C?
}