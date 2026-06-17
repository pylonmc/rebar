package io.github.pylonmc.rebar.electricity

import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.util.FourTuple
import io.github.pylonmc.rebar.util.position.BlockPosition
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import java.util.*
import java.util.function.Consumer as ConsumerFn

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

        ElectricityManager.mergeNetworks()
    }

    fun isConnectedTo(other: ElectricNode) = other.id in internalConnections

    fun disconnectFrom(other: ElectricNode) {
        internalConnections.remove(other.id)
        other.internalConnections.remove(this.id)

        onDisconnect.handle(this, other)
        other.onDisconnect.handle(other, this)
        ElectricityManager.refreshNetworks(network, other.network)
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

    protected fun serializeCommon(pdc: PersistentDataContainer) {
        pdc.set(ID_KEY, RebarSerializers.UUID, id)
        pdc.set(NAME_KEY, RebarSerializers.STRING, name)
        pdc.set(BLOCK_KEY, RebarSerializers.BLOCK_POSITION, block)
        pdc.set(CONNECTIONS_KEY, CONNECTIONS_TYPE, internalConnections)
    }

    class Connector private constructor(
        id: UUID,
        name: String,
        block: BlockPosition,
        internalConnections: MutableSet<UUID>
    ) : ElectricNode(id, name, block, internalConnections) {

        constructor(
            name: String,
            block: BlockPosition
        ) : this(UUID.randomUUID(), name, block, mutableSetOf())

        companion object {

            @get:JvmSynthetic
            internal val PDC_TYPE = object : PersistentDataType<PersistentDataContainer, Connector> {
                override fun getPrimitiveType() = PersistentDataContainer::class.java
                override fun getComplexType() = Connector::class.java

                override fun toPrimitive(
                    complex: Connector,
                    context: PersistentDataAdapterContext
                ): PersistentDataContainer {
                    val pdc = context.newPersistentDataContainer()
                    complex.serializeCommon(pdc)
                    return pdc
                }

                override fun fromPrimitive(
                    primitive: PersistentDataContainer,
                    context: PersistentDataAdapterContext
                ): Connector {
                    val (id, name, block, internalConnections) = deserializeCommon(primitive)
                    return Connector(id, name, block, internalConnections)
                }
            }
        }
    }

    class Producer private constructor(
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
        internal var powerTakeHandler = ConsumerFn<Double> { }

        fun onPowerTake(handler: ConsumerFn<Double>) {
            powerTakeHandler = handler
        }

        companion object {

            private val POWER_KEY = rebarKey("power")

            @get:JvmSynthetic
            internal val PDC_TYPE = object : PersistentDataType<PersistentDataContainer, Producer> {
                override fun getPrimitiveType() = PersistentDataContainer::class.java
                override fun getComplexType() = Producer::class.java

                override fun toPrimitive(
                    complex: Producer,
                    context: PersistentDataAdapterContext
                ): PersistentDataContainer {
                    val pdc = context.newPersistentDataContainer()
                    complex.serializeCommon(pdc)
                    pdc.set(POWER_KEY, RebarSerializers.DOUBLE, complex.power)
                    return pdc
                }

                override fun fromPrimitive(
                    primitive: PersistentDataContainer,
                    context: PersistentDataAdapterContext
                ): Producer {
                    val (id, name, block, internalConnections) = deserializeCommon(primitive)
                    val power = primitive.get(POWER_KEY, RebarSerializers.DOUBLE)!!
                    return Producer(id, name, block, internalConnections, power)
                }
            }
        }
    }

    class Consumer private constructor(
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

        companion object {

            private val REQUIRED_POWER_KEY = rebarKey("required_power")

            @get:JvmSynthetic
            internal val PDC_TYPE = object : PersistentDataType<PersistentDataContainer, Consumer> {
                override fun getPrimitiveType() = PersistentDataContainer::class.java
                override fun getComplexType() = Consumer::class.java

                override fun toPrimitive(
                    complex: Consumer,
                    context: PersistentDataAdapterContext
                ): PersistentDataContainer {
                    val pdc = context.newPersistentDataContainer()
                    complex.serializeCommon(pdc)
                    pdc.set(REQUIRED_POWER_KEY, RebarSerializers.DOUBLE, complex.requiredPower)
                    return pdc
                }

                override fun fromPrimitive(
                    primitive: PersistentDataContainer,
                    context: PersistentDataAdapterContext
                ): Consumer {
                    val (id, name, block, internalConnections) = deserializeCommon(primitive)
                    val requiredPower = primitive.get(REQUIRED_POWER_KEY, RebarSerializers.DOUBLE)!!
                    return Consumer(id, name, block, internalConnections, requiredPower)
                }
            }
        }
    }
    class Acceptor private constructor(
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

        companion object {

            @get:JvmSynthetic
            internal val PDC_TYPE = object : PersistentDataType<PersistentDataContainer, Acceptor> {
                override fun getPrimitiveType() = PersistentDataContainer::class.java
                override fun getComplexType() = Acceptor::class.java

                override fun toPrimitive(
                    complex: Acceptor,
                    context: PersistentDataAdapterContext
                ): PersistentDataContainer {
                    val pdc = context.newPersistentDataContainer()
                    complex.serializeCommon(pdc)
                    return pdc
                }

                override fun fromPrimitive(
                    primitive: PersistentDataContainer,
                    context: PersistentDataAdapterContext
                ): Acceptor {
                    val (id, name, block, internalConnections) = deserializeCommon(primitive)
                    return Acceptor(id, name, block, internalConnections)
                }
            }
        }
    }

    data class Edge(val from: ElectricNode, val to: ElectricNode)

    companion object {

        private val ID_KEY = rebarKey("id")
        private val NAME_KEY = rebarKey("name")
        private val BLOCK_KEY = rebarKey("block")
        private val CONNECTIONS_KEY = rebarKey("connections")
        private val CONNECTIONS_TYPE = RebarSerializers.SET.setTypeFrom(RebarSerializers.UUID)

        @get:JvmSynthetic
        internal val PDC_TYPE =
            RebarSerializers.POLYMORPHIC.of(Producer.PDC_TYPE, Consumer.PDC_TYPE, Connector.PDC_TYPE, Acceptor.PDC_TYPE)

        private fun deserializeCommon(pdc: PersistentDataContainer): FourTuple<UUID, String, BlockPosition, MutableSet<UUID>> {
            val id = pdc.get(ID_KEY, RebarSerializers.UUID)!!
            val name = pdc.get(NAME_KEY, RebarSerializers.STRING)!!
            val block = pdc.get(BLOCK_KEY, RebarSerializers.BLOCK_POSITION)!!
            val connections = pdc.get(CONNECTIONS_KEY, CONNECTIONS_TYPE)!!.toMutableSet()
            return FourTuple(id, name, block, connections)
        }
    }
}