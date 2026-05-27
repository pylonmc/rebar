package io.github.pylonmc.rebar.electricity

import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.datatypes.map
import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.world.WorldStorage
import org.bukkit.persistence.PersistentDataType

sealed interface EdgeProperty<V> {
    val value: V

    data class PowerLimit(override val value: Double) : EdgeProperty<Double> {
        companion object {
            @get:JvmSynthetic
            internal val PDC_TYPE = RebarSerializers.DOUBLE.map(
                from = ::PowerLimit,
                to = PowerLimit::value
            )
        }
    }

    data object Unidirectional : EdgeProperty<Unit> {
        override val value = Unit

        @get:JvmSynthetic
        internal val PDC_TYPE = RebarSerializers.BOOLEAN.map(
            from = { Unidirectional },
            to = { true }
        )
    }

    companion object {

        private val PDC_TYPE: PersistentDataType<*, EdgeProperty<*>> =
            RebarSerializers.POLYMORPHIC.of(PowerLimit.PDC_TYPE, Unidirectional.PDC_TYPE)

        private val PROPERTIES_TYPE = RebarSerializers.SET.setTypeFrom(PDC_TYPE)

        @JvmStatic
        fun getProperties(edge: ElectricNode.Edge): Set<EdgeProperty<*>> {
            val world = edge.from.block.world ?: return emptySet()
            val storage = WorldStorage.getStorage(world)
            val key = rebarKey("edge_properties_${edge.from.id}_${edge.to.id}")
            return storage[key, PROPERTIES_TYPE] ?: emptySet()
        }

        @JvmStatic
        fun <T : EdgeProperty<*>> getProperty(edge: ElectricNode.Edge, propertyClass: Class<T>): T? {
            @Suppress("UNCHECKED_CAST")
            return getProperties(edge).firstOrNull { it::class.java == propertyClass } as? T
        }

        @JvmSynthetic
        inline fun <reified T : EdgeProperty<*>> getProperty(edge: ElectricNode.Edge): T? {
            return getProperty(edge, T::class.java)
        }

        @JvmStatic
        fun setProperty(edge: ElectricNode.Edge, property: EdgeProperty<*>) {
            val world = edge.from.block.world ?: return
            val storage = WorldStorage.getStorage(world)
            val key = rebarKey("edge_properties_${edge.from.id}_${edge.to.id}")
            val properties = storage[key, PROPERTIES_TYPE]?.toMutableSet() ?: mutableSetOf()
            properties.removeIf { it::class == property::class }
            properties.add(property)
            storage[key, PROPERTIES_TYPE] = properties
        }

        @JvmStatic
        fun removeProperty(edge: ElectricNode.Edge, propertyClass: Class<out EdgeProperty<*>>): Boolean {
            val world = edge.from.block.world ?: return false
            val storage = WorldStorage.getStorage(world)
            val key = rebarKey("edge_properties_${edge.from.id}_${edge.to.id}")
            val properties = storage[key, PROPERTIES_TYPE]?.toMutableSet() ?: return false
            val removed = properties.removeIf { it::class.java == propertyClass }
            if (removed) {
                storage[key, PROPERTIES_TYPE] = properties
            }
            return removed
        }

        @JvmSynthetic
        inline fun <reified T : EdgeProperty<*>> removeProperty(edge: ElectricNode.Edge): Boolean {
            return removeProperty(edge, T::class.java)
        }
    }
}