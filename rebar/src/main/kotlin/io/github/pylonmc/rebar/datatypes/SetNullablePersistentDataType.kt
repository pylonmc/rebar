package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.persistence.ListPersistentDataType
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

/**
 * A [SetPersistentDataType] that allows null values to be inputted,
 * but eventually null values will be filtered out rather than being preserved
 *
 * @see NullablePersistentDataType
 */
class SetNullablePersistentDataType<P, C>(
    val elementType: NullablePersistentDataType<P, C>
) : PersistentDataType<PersistentDataContainer, Set<C>> {

    private val setValues = rebarKey("values")

    override fun getPrimitiveType(): Class<PersistentDataContainer> = PersistentDataContainer::class.java

    @Suppress("UNCHECKED_CAST") // Yes, this is cursed; no, there's no way around it afaik
    override fun getComplexType(): Class<Set<C>> = Set::class.java as Class<Set<C>>

    override fun toPrimitive(complex: Set<C>, context: PersistentDataAdapterContext): PersistentDataContainer {
        val pdc = context.newPersistentDataContainer()
        pdc.set(setValues, RebarSerializers.LIST_NULLABLE.listTypeFrom(elementType), ArrayList(complex))
        return pdc
    }

    override fun fromPrimitive(primitive: PersistentDataContainer, context: PersistentDataAdapterContext): Set<C> =
        primitive.get(setValues, RebarSerializers.LIST_NULLABLE.listTypeFrom(elementType))!!.toMutableSet()

    companion object {
        fun <P, C> setTypeFrom(elementType: NullablePersistentDataType<P, C>): PersistentDataType<PersistentDataContainer, Set<C>> =
            SetNullablePersistentDataType(elementType)
    }
}
