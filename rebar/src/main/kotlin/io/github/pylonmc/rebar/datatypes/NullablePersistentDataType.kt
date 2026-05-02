package io.github.pylonmc.rebar.datatypes

import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType

/**
 * An interface for [PersistentDataType] implementations that support nullable conversions.
 * This interface should only be used within nested PersistentDataType classes (such as
 * [ListNullablePersistentDataType] and [SetNullablePersistentDataType]) to handle cases where
 * individual elements within a collection may be null or fail conversion.
 *
 * In [ListNullablePersistentDataType] and [SetNullablePersistentDataType], null values
 * are filtered out during deserialization rather than being preserved. When [fromPrimitiveNullable]
 * returns null, that element is simply removed from the resulting collection.
 *
 * Do not use this interface directly in top-level PersistentDataType implementations.
 *
 * @see SetNullablePersistentDataType
 * @see ListNullablePersistentDataType
 */
interface NullablePersistentDataType<P, C> : PersistentDataType<P, C> {
    /**
     * Converts a primitive value to its complex representation.
     * Unlike [fromPrimitive], this method is allowed to return null when the conversion fails
     * or when the primitive value does not correspond to a valid complex object.
     *
     * @return The converted complex object, or null if conversion is not possible
     * @see PersistentDataType.fromPrimitive
     */
    fun fromPrimitiveNullable(primitive: P, context: PersistentDataAdapterContext): C?
}