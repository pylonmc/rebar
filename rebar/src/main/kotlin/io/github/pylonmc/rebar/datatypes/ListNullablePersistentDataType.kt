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
        val result = mutableListOf<C>()
        primitive.forEach { it -> {
            @Suppress("UNCHECKED_CAST")
            val nullable = innerType as? NullablePersistentDataType<P, C>
            if (nullable != null) {
                val r = innerType.fromPrimitiveNullable(it!!, context)
                if (r != null) result.add(r)
            } else {
                result.add(innerType.fromPrimitive(it!!, context))
            }
        }}
        return result
    }

    override fun elementType(): PersistentDataType<P, C> = innerType

    companion object {
        fun <P, C> listTypeFrom(innerType: PersistentDataType<P, C>): PersistentDataType<List<P>, List<C>> =
            ListNullablePersistentDataType(innerType)
    }
}