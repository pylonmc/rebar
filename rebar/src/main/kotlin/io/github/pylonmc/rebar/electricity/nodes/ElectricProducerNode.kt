package io.github.pylonmc.rebar.electricity.nodes

import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.util.position.BlockPosition
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.persistence.PersistentDataContainer
import java.util.*
import java.util.function.Consumer

class ElectricProducerNode private constructor(
    id: UUID,
    name: String,
    block: BlockPosition,
    internalConnections: MutableSet<UUID>,
    power: Double,
    priority: Int
) : ElectricNode(id, name, block, internalConnections) {

    @JvmOverloads
    constructor(
        name: String,
        block: BlockPosition,
        power: Double,
        priority: Int = 0
    ) : this(UUID.randomUUID(), name, block, mutableSetOf(), power, priority)

    /**
     * The amount of power that this producer produces, measured in watts.
     */
    var power = power
        set(value) {
            if (field != value) {
                network.producerChangedPower(this, value, value > field)
            }
            field = value
        }

    /**
     * Higher priority means the producer will be less prioritized when taking power from
     */
    var priority = priority
        set(value) {
            if (field != value) {
                network.markDirty()
            }
            field = value
        }

    @get:JvmSynthetic
    @set:JvmSynthetic
    internal var powerTakeHandler = Consumer<Double> { }

    fun onPowerTake(handler: Consumer<Double>) {
        powerTakeHandler = handler
    }

    override fun serialize(pdc: PersistentDataContainer) {
        pdc.set(POWER_KEY, RebarSerializers.DOUBLE, power)
        pdc.set(PRIORITY_KEY, RebarSerializers.INTEGER, priority)
    }

    companion object {

        private val POWER_KEY = rebarKey("power")
        private val PRIORITY_KEY = rebarKey("priority")

        @JvmSynthetic
        internal fun deserialize(
            id: UUID,
            name: String,
            block: BlockPosition,
            internalConnections: MutableSet<UUID>,
            pdc: PersistentDataContainer
        ): ElectricProducerNode {
            val power = pdc.get(POWER_KEY, RebarSerializers.DOUBLE)!!
            val priority = pdc.get(PRIORITY_KEY, RebarSerializers.INTEGER)!!
            return ElectricProducerNode(id, name, block, internalConnections, power, priority)
        }
    }
}