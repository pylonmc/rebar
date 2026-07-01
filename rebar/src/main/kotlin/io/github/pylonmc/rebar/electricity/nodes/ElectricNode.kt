package io.github.pylonmc.rebar.electricity.nodes

import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.electricity.ElectricNetwork
import io.github.pylonmc.rebar.electricity.ElectricityManager
import io.github.pylonmc.rebar.util.position.BlockPosition
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import java.util.*

/**
 * Represents a node in an electric network. This can be a producer, consumer, connector, or acceptor of power.
 */
sealed class ElectricNode(
    val id: UUID,
    val name: String,
    val block: BlockPosition,
    private val internalConnections: MutableSet<UUID>
) {

    /**
     * The IDs of the nodes that this node is directly connected to. Said nodes may not be loaded, which is why we store
     * the IDs and not references to the nodes themselves.
     */
    val connections: Set<UUID> get() = internalConnections.toSet()

    /**
     * The network of this node.
     */
    val network: ElectricNetwork get() = ElectricityManager.getNodeNetwork(this)

    protected var onConnect = ConnectDisconnectHandler { _, _ -> }
    protected var onDisconnect = ConnectDisconnectHandler { _, _ -> }

    fun connect(other: ElectricNode) {
        internalConnections.add(other.id)
        other.internalConnections.add(this.id)

        onConnect.handle(this, other)
        other.onConnect.handle(other, this)

        ElectricityManager.mergeNetworks(listOf(this.network, other.network))
    }

    fun isConnectedTo(other: ElectricNode) = other.id in internalConnections

    fun disconnectFrom(other: ElectricNode) {
        internalConnections.remove(other.id)
        other.internalConnections.remove(this.id)

        onDisconnect.handle(this, other)
        other.onDisconnect.handle(other, this)
        ElectricityManager.refreshNetwork(network)
    }

    fun disconnectAll() {
        for (connection in internalConnections.toList()) {
            ElectricityManager.getNodeById(connection)?.let { disconnectFrom(it) }
        }
        internalConnections.clear()
    }

    /**
     * Registers a handler to be called when a connection is made between this node and another node.
     *
     * Passing a new handler will not replace the old one, but will cause both the old and new handlers to be called when a connection is made.
     */
    fun onConnect(handler: ConnectDisconnectHandler) {
        val oldOnConnect = onConnect
        onConnect = { n1, n2 ->
            oldOnConnect.handle(n1, n2)
            handler.handle(n1, n2)
        }
    }

    /**
     * Registers a handler to be called when a connection is broken between this node and another node.
     *
     * Passing a new handler will not replace the old one, but will cause both the old and new handlers to be called when a connection is broken.
     */
    fun onDisconnect(handler: ConnectDisconnectHandler) {
        val oldOnDisconnect = onDisconnect
        onDisconnect = { n1, n2 ->
            oldOnDisconnect.handle(n1, n2)
            handler.handle(n1, n2)
        }
    }

    @FunctionalInterface
    fun interface ConnectDisconnectHandler {
        /**
         * Called when a connection is made or broken between two nodes, depending on what it was registered.
         *
         * @param node the node that this handler is registered on
         * @param other the other node that is being connected to or disconnected from
         */
        fun handle(node: ElectricNode, other: ElectricNode)
    }

    override fun equals(other: Any?) = other is ElectricNode && id == other.id

    override fun hashCode() = id.hashCode()

    override fun toString(): String {
        return "Electric node \"$name\" of type ${this::class.simpleName} at $block with ID $id"
    }

    protected abstract fun serialize(pdc: PersistentDataContainer)

    companion object {

        private val TYPE_KEY = rebarKey("type")
        private val ID_KEY = rebarKey("id")
        private val NAME_KEY = rebarKey("name")
        private val BLOCK_KEY = rebarKey("block")
        private val CONNECTIONS_KEY = rebarKey("connections")
        private val CONNECTIONS_TYPE = RebarSerializers.SET.setTypeFrom(RebarSerializers.UUID)

        @get:JvmSynthetic
        internal val PDC_TYPE = object : PersistentDataType<PersistentDataContainer, ElectricNode> {
            override fun getPrimitiveType() = PersistentDataContainer::class.java
            override fun getComplexType() = ElectricNode::class.java

            override fun toPrimitive(
                complex: ElectricNode,
                context: PersistentDataAdapterContext
            ): PersistentDataContainer {
                val pdc = context.newPersistentDataContainer()

                val type = when (complex) {
                    is ElectricProducerNode -> "producer"
                    is ElectricConsumerNode -> "consumer"
                    is ElectricConnectorNode -> "connector"
                    is ElectricAcceptorNode -> "acceptor"
                }
                pdc.set(TYPE_KEY, RebarSerializers.STRING, type)

                pdc.set(ID_KEY, RebarSerializers.UUID, complex.id)
                pdc.set(NAME_KEY, RebarSerializers.STRING, complex.name)
                pdc.set(BLOCK_KEY, RebarSerializers.BLOCK_POSITION, complex.block)
                pdc.set(CONNECTIONS_KEY, CONNECTIONS_TYPE, complex.internalConnections)

                complex.serialize(pdc)

                return pdc
            }

            override fun fromPrimitive(
                primitive: PersistentDataContainer,
                context: PersistentDataAdapterContext
            ): ElectricNode {
                val id = primitive.get(ID_KEY, RebarSerializers.UUID)!!
                val name = primitive.get(NAME_KEY, RebarSerializers.STRING)!!
                val block = primitive.get(BLOCK_KEY, RebarSerializers.BLOCK_POSITION)!!
                val connections = primitive.get(CONNECTIONS_KEY, CONNECTIONS_TYPE)!!.toMutableSet()

                return when (primitive.get(TYPE_KEY, RebarSerializers.STRING)!!) {
                    "producer" -> ElectricProducerNode.deserialize(id, name, block, connections, primitive)
                    "consumer" -> ElectricConsumerNode.deserialize(id, name, block, connections, primitive)
                    "connector" -> ElectricConnectorNode.deserialize(id, name, block, connections)
                    "acceptor" -> ElectricAcceptorNode.deserialize(id, name, block, connections)
                    else -> throw AssertionError()
                }
            }
        }
    }
}