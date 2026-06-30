package io.github.pylonmc.rebar.datatypes

import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType

/**
 * A [PersistentDataType] that first converts the complex type
 * to another type, which then uses the delegate to convert to the primitive type.
 *
 * @param P the primitive type
 * @param D the delegated type
 * @param C the complex type
 */
abstract class DelegatingPersistentDataType<P : Any, D : Any, C : Any>(
    val delegate: PersistentDataType<P, D>,
) : PersistentDataType<P, C> {

    final override fun getPrimitiveType(): Class<P> = delegate.primitiveType

    abstract fun toDelegatedType(complex: C): D

    abstract fun fromDelegatedType(primitive: D): C

    final override fun toPrimitive(complex: C, context: PersistentDataAdapterContext): P {
        return delegate.toPrimitive(toDelegatedType(complex), context)
    }

    final override fun fromPrimitive(primitive: P, context: PersistentDataAdapterContext): C {
        return fromDelegatedType(delegate.fromPrimitive(primitive, context))
    }
}