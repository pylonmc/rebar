package io.github.pylonmc.rebar.electricity.nodes

import io.github.pylonmc.rebar.util.position.BlockPosition
import org.bukkit.persistence.PersistentDataContainer
import java.util.*

class ElectricConnectorNode private constructor(
    id: UUID,
    name: String,
    block: BlockPosition,
    internalConnections: MutableSet<UUID>
) : ElectricNode(id, name, block, internalConnections) {

    constructor(
        name: String,
        block: BlockPosition
    ) : this(UUID.randomUUID(), name, block, mutableSetOf())

    override fun serialize(pdc: PersistentDataContainer) {
    }

    companion object {

        @JvmSynthetic
        internal fun deserialize(
            id: UUID,
            name: String,
            block: BlockPosition,
            internalConnections: MutableSet<UUID>
        ): ElectricConnectorNode {
            return ElectricConnectorNode(id, name, block, internalConnections)
        }
    }
}