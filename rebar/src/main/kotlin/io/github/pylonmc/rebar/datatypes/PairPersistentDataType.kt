package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

class PairPersistentDataType<F : Any, S : Any>(
    val first: PersistentDataType<*, F>,
    val second: PersistentDataType<*, S>
) : PersistentDataType<PersistentDataContainer, Pair<F, S>> {

    override fun getPrimitiveType() = PersistentDataContainer::class.java

    @Suppress("UNCHECKED_CAST")
    override fun getComplexType() = Pair::class.java as Class<Pair<F, S>>

    override fun toPrimitive(complex: Pair<F, S>, context: PersistentDataAdapterContext): PersistentDataContainer {
        val container = context.newPersistentDataContainer()
        container.set(firstKey, first, complex.first)
        container.set(secondKey, second, complex.second)
        return container
    }

    override fun fromPrimitive(primitive: PersistentDataContainer, context: PersistentDataAdapterContext): Pair<F, S> {
        val firstValue = primitive.get(firstKey, first)!!
        val secondValue = primitive.get(secondKey, second)!!
        return Pair(firstValue, secondValue)
    }

    companion object {
        private val firstKey = rebarKey("first")
        private val secondKey = rebarKey("second")

        @JvmStatic
        fun <F : Any, S : Any> pairTypeFrom(
            first: PersistentDataType<*, F>,
            second: PersistentDataType<*, S>
        ): PersistentDataType<PersistentDataContainer, Pair<F, S>> = PairPersistentDataType(first, second)
    }
}