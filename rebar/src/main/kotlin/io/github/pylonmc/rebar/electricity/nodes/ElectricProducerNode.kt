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
    /**
     * The amount of power that this producer produces, measured in watts.
     */
    var power: Double
) : ElectricNode(id, name, block, internalConnections) {
    constructor(
        name: String,
        block: BlockPosition,
        power: Double
    ) : this(UUID.randomUUID(), name, block, mutableSetOf(), power)

    @get:JvmSynthetic
    @set:JvmSynthetic
    internal var powerTakeHandler = Consumer<Double> { }

    fun onPowerTake(handler: Consumer<Double>) {
        powerTakeHandler = handler
    }

    override fun serialize(pdc: PersistentDataContainer) {
        pdc.set(POWER_KEY, RebarSerializers.DOUBLE, power)
    }

    companion object {

        private val POWER_KEY = rebarKey("power")

        @JvmSynthetic
        internal fun deserialize(
            id: UUID,
            name: String,
            block: BlockPosition,
            internalConnections: MutableSet<UUID>,
            pdc: PersistentDataContainer
        ): ElectricProducerNode {
            val power = pdc.get(POWER_KEY, RebarSerializers.DOUBLE)!!
            return ElectricProducerNode(id, name, block, internalConnections, power)
        }
    }
}