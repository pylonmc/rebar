package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

/**
 * A [PersistentDataType] that can serialize multiple different types, as long as they all share a common supertype
 */
class PolymorphicPersistentDataType<T : Any>(types: List<PersistentDataType<*, out T>>) : PersistentDataType<PersistentDataContainer, T> {

    private val supertype: Class<*> = types.map { it.getComplexType() as Class<*> }.reduce { acc, clazz -> acc.findCommonSupertype(clazz) }
    private val classToType = types.associateBy { it.getComplexType() as Class<*> }
    private val tagToType = classToType.mapKeys { (key, _) -> key.name }

    override fun getPrimitiveType() = PersistentDataContainer::class.java

    @Suppress("UNCHECKED_CAST")
    override fun getComplexType() = supertype as Class<T>

    override fun toPrimitive(
        complex: T,
        context: PersistentDataAdapterContext
    ): PersistentDataContainer {
        val clazz = complex::class.java
        val type = classToType[clazz] ?: throw IllegalArgumentException("Unsupported type: ${clazz.name}")
        val pdc = context.newPersistentDataContainer()
        pdc.set(TAG_KEY, PersistentDataType.STRING, clazz.name)
        @Suppress("UNCHECKED_CAST")
        pdc.set(VALUE_KEY, type as PersistentDataType<*, T>, complex)
        return pdc
    }

    override fun fromPrimitive(
        primitive: PersistentDataContainer,
        context: PersistentDataAdapterContext
    ): T {
        val tag = primitive.get(TAG_KEY, PersistentDataType.STRING)!!
        val type = tagToType[tag] ?: throw IllegalArgumentException("Unsupported tag: $tag")
        return primitive.get(VALUE_KEY, type)!!
    }

    companion object {
        private val TAG_KEY = rebarKey("tag")
        private val VALUE_KEY = rebarKey("value")

        @JvmStatic
        fun <T : Any> of(vararg types: PersistentDataType<*, out T>): PersistentDataType<PersistentDataContainer, T> {
            return PolymorphicPersistentDataType(types.toList())
        }

        @JvmStatic
        fun <T : Any> of(types: Collection<PersistentDataType<*, out T>>): PersistentDataType<PersistentDataContainer, T> {
            return PolymorphicPersistentDataType(types.toList())
        }
    }
}

private fun Class<*>.findCommonSupertype(other: Class<*>): Class<*> {
    if (this == other) return this

    val thisSupers = generateSequence(this) { it.superclass }.toSet()
    val otherSupers = generateSequence(other) { it.superclass }.toSet()

    return thisSupers.intersect(otherSupers).first()
}