package io.github.pylonmc.rebar.electricity

import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.Location
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import java.util.UUID
import java.util.function.BiConsumer

class ElectricNode private constructor(
    val id: UUID,
    private val actualConnectionPoint: Location,
    val type: Type,
    private val connectionSet: MutableSet<UUID>
) {

    constructor(connectionPoint: Location, type: Type) : this(UUID.randomUUID(), connectionPoint, type, mutableSetOf())

    val connections: Set<UUID> get() = connectionSet.toSet()

    val connectionPoint get() = actualConnectionPoint.clone()

    private var onConnect: BiConsumer<ElectricNode, ElectricNode> = BiConsumer { _, _ -> }
    private var onDisconnect: BiConsumer<ElectricNode, ElectricNode> = BiConsumer { _, _ -> }

    /**
     * Throws an error if this node is not part of any network, which shouldn't happen if the node was added to the [ElectricityManager]
     */
    val network: ElectricNetwork get() = ElectricityManager.getNodeNetwork(this)

    fun connect(other: ElectricNode) {
        connectionSet.add(other.id)
        other.connectionSet.add(this.id)
        ElectricityManager.mergeNetworks()
        onConnect.accept(this, other)
        other.onConnect.accept(other, this)
    }

    fun disconnect(other: ElectricNode) {
        connectionSet.remove(other.id)
        other.connectionSet.remove(this.id)
        ElectricityManager.refreshNetworks(network, other.network)
        onDisconnect.accept(this, other)
        other.onDisconnect.accept(other, this)
    }

    fun isConnectedTo(other: ElectricNode): Boolean = other.id in connectionSet

    fun onConnect(listener: BiConsumer<ElectricNode, ElectricNode>) {
        onConnect = listener
    }

    fun onDisconnect(listener: BiConsumer<ElectricNode, ElectricNode>) {
        onDisconnect = listener
    }

    override fun equals(other: Any?): Boolean = other is ElectricNode && other.id == this.id
    override fun hashCode(): Int = id.hashCode()

    enum class Type {
        PRODUCER,
        CONSUMER,
        CONNECTOR
    }

    companion object {
        @JvmSynthetic
        internal val PDC_TYPE = object : PersistentDataType<PersistentDataContainer, ElectricNode> {

            private val id = rebarKey("id")
            private val location = rebarKey("location")
            private val type = rebarKey("type")
            private val connections = rebarKey("connections")

            private val typeType = RebarSerializers.ENUM.enumTypeFrom<Type>()
            private val connectionsType = RebarSerializers.SET.setTypeFrom(RebarSerializers.UUID)

            override fun getPrimitiveType(): Class<PersistentDataContainer> = PersistentDataContainer::class.java
            override fun getComplexType(): Class<ElectricNode> = ElectricNode::class.java

            override fun toPrimitive(
                complex: ElectricNode,
                context: PersistentDataAdapterContext
            ): PersistentDataContainer {
                val container = context.newPersistentDataContainer()
                container.set(id, RebarSerializers.UUID, complex.id)
                container.set(location, RebarSerializers.LOCATION, complex.connectionPoint)
                container.set(type, typeType, complex.type)
                container.set(connections, connectionsType, complex.connectionSet)
                return container
            }

            override fun fromPrimitive(
                primitive: PersistentDataContainer,
                context: PersistentDataAdapterContext
            ): ElectricNode {
                val id = primitive.get(id, RebarSerializers.UUID)!!
                val location = primitive.get(location, RebarSerializers.LOCATION)!!
                val type = primitive.get(type, typeType)!!
                val connections = primitive.get(connections, connectionsType)!!
                return ElectricNode(id, location, type, connections.toMutableSet())
            }
        }
    }
}