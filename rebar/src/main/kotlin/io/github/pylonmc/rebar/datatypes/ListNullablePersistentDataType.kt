package io.github.pylonmc.rebar.datatypes

import org.bukkit.persistence.ListPersistentDataType
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType

class ListNullablePersistentDataType<P, C>(
    private val innerType: PersistentDataType<P, C>
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
        val nullable = innerType as? NullablePersistentDataType<P, C>
        return primitive.mapNotNull {
            if (nullable != null)
                nullable.fromPrimitiveNullable(it!!, context)
            else 
                innerType.fromPrimitive(it!!, context)
        }
    }

    override fun elementType(): PersistentDataType<P, C> = innerType

    companion object {
        fun <P, C> listTypeFrom(innerType: PersistentDataType<P, C>): PersistentDataType<List<P>, List<C>> =
            ListNullablePersistentDataType(innerType)
    }
}