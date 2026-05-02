package io.github.pylonmc.rebar.datatypes

import org.bukkit.persistence.PersistentDataAdapterContext

interface NullablePersistentDataType<P, C> {
    fun fromPrimitiveNullable(primitive: P, context: PersistentDataAdapterContext): C?

    fun fromPrimitive(primitive: P, context: PersistentDataAdapterContext): C
}