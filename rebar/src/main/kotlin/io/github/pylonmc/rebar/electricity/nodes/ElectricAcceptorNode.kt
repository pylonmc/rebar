package io.github.pylonmc.rebar.electricity.nodes

import io.github.pylonmc.rebar.util.position.BlockPosition
import org.bukkit.persistence.PersistentDataContainer
import java.util.*

class ElectricAcceptorNode private constructor(
    id: UUID,
    name: String,
    block: BlockPosition,
    internalConnections: MutableSet<UUID>,
) : ElectricNode(id, name, block, internalConnections) {

    constructor(
        name: String,
        block: BlockPosition
    ) : this(UUID.randomUUID(), name, block, mutableSetOf())

    @get:JvmSynthetic
    @set:JvmSynthetic
    internal var handler = AcceptorHandler { 0.0 }


    /**
     * Registers a handler, which is called with surplus power provided by the network.
     */
    fun onAccept(handler: AcceptorHandler) {
        this.handler = handler
    }

    @FunctionalInterface
    fun interface AcceptorHandler {

        /**
         * Called when the acceptor is accepting power. The returned value is the amount of power that was accepted,
         * which may be less than the required power if the acceptor cannot accept all of it.
         */
        fun onAccept(power: Double): Double
    }

    override fun serialize(pdc: PersistentDataContainer) {
    }

    companion object {

        @JvmSynthetic
        internal fun deserialize(
            id: UUID,
            name: String,
            block: BlockPosition,
            internalConnections: MutableSet<UUID>
        ): ElectricAcceptorNode {
            return ElectricAcceptorNode(id, name, block, internalConnections)
        }
    }
}