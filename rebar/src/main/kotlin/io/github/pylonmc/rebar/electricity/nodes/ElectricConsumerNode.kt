package io.github.pylonmc.rebar.electricity.nodes

import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.util.position.BlockPosition
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.persistence.PersistentDataContainer
import java.util.*

class ElectricConsumerNode private constructor(
    id: UUID,
    name: String,
    block: BlockPosition,
    internalConnections: MutableSet<UUID>,
    /**
     * The amount of power that this consumer requires, measured in watts. Should the network not be able to provide this
     * many watts, [isPowered] will be false and the consumer should not operate.
     */
    var requiredPower: Double
) : ElectricNode(id, name, block, internalConnections) {

    constructor(
        name: String,
        block: BlockPosition,
        requiredPower: Double
    ) : this(UUID.randomUUID(), name, block, mutableSetOf(), requiredPower)

    /**
     * Returns `true` if this consumer is receiving at least as much power as it requires, and `false` otherwise.
     */
    var isPowered: Boolean = false
        @JvmSynthetic
        internal set

    override fun serialize(pdc: PersistentDataContainer) {
        pdc.set(REQUIRED_POWER_KEY, RebarSerializers.DOUBLE, requiredPower)
    }

    companion object {

        private val REQUIRED_POWER_KEY = rebarKey("required_power")

        @JvmSynthetic
        internal fun deserialize(
            id: UUID,
            name: String,
            block: BlockPosition,
            internalConnections: MutableSet<UUID>,
            pdc: PersistentDataContainer
        ): ElectricConsumerNode {
            val requiredPower = pdc.get(REQUIRED_POWER_KEY, RebarSerializers.DOUBLE)!!
            return ElectricConsumerNode(id, name, block, internalConnections, requiredPower)
        }
    }
}