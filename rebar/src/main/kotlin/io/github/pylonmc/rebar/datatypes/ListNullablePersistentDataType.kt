package io.github.pylonmc.rebar.datatypes

import org.bukkit.persistence.ListPersistentDataType
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType

/**
 * A [ListPersistentDataType] that allows null values to be inputted,
 * but eventually null values will be filtered out rather than being preserved
 *
 * @see NullablePersistentDataType
 */
class ListNullablePersistentDataType<P, C>(
    private val innerType: NullablePersistentDataType<P, C>
) : ListPersistentDataType<P, C> {

    @Suppress("UNCHECKED_CAST")
    override fun getPrimitiveType(): Class<List<P>> = List::class.java as Class<List<P>>

    @Suppress("UNCHECKED_CAST")
    override fun getComplexType(): Class<List<C>> = List::class.java as Class<List<C>>

    override fun toPrimitive(complex: List<C>, context: PersistentDataAdapterContext): List<P> {
        return complex.map { innerType.toPrimitive(it!!, context)}
    }

    override fun fromPrimitive(primitive: List<P>, context: PersistentDataAdapterContext): List<C> {
        @Suppress("UNCHECKED_CAST")
        return primitive.mapNotNull { innerType.fromPrimitiveNullable(it!!, context) }
    }

    override fun elementType(): NullablePersistentDataType<P, C> = innerType

    companion object {
        fun <P, C> listTypeFrom(innerType: NullablePersistentDataType<P, C>): PersistentDataType<List<P>, List<C>> =
            ListNullablePersistentDataType(innerType)
    }
}