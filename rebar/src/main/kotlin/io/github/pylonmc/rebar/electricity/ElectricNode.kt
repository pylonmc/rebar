package io.github.pylonmc.rebar.electricity

import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.datatypes.map
import io.github.pylonmc.rebar.util.position.BlockPosition
import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.util.setNullable
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.jetbrains.annotations.ApiStatus
import java.util.*
import java.util.function.Consumer as ConsumerFn

sealed class ElectricNode(
    val id: UUID,
    val name: String,
    val block: BlockPosition
) {

    val network: ElectricNetwork get() = ElectricityManager.getNodeNetwork(this)

    protected var onConnect = ConnectDisconnectHandler { _, _ -> }
    protected var onDisconnect = ConnectDisconnectHandler { _, _ -> }

    abstract fun connect(other: ElectricNode)
    abstract fun isConnectedTo(other: ElectricNode): Boolean
    abstract fun disconnectFrom(other: ElectricNode)

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

    class Connector private constructor(
        id: UUID,
        name: String,
        block: BlockPosition,
        @get:JvmSynthetic internal val connectionSet: MutableSet<UUID>,
    ) : ElectricNode(id, name, block) {

        constructor(
            name: String,
            block: BlockPosition
        ) : this(UUID.randomUUID(), name, block, mutableSetOf())

        val connections get() = connectionSet.toSet()

        override fun connect(other: ElectricNode) {
            when (other) {
                is Leaf -> other.connect(this)
                is Connector -> {
                    connectionSet.add(other.id)
                    other.connectionSet.add(this.id)

                    onConnect.handle(this, other)
                    other.onConnect.handle(other, this)

                    ElectricityManager.mergeNetworks()
                }
            }
        }

        override fun isConnectedTo(other: ElectricNode) = other.id in connectionSet

        override fun disconnectFrom(other: ElectricNode) {
            when (other) {
                is Leaf -> {
                    other.disconnect()
                }

                is Connector -> {
                    connectionSet.remove(other.id)
                    other.connectionSet.remove(this.id)

                    onDisconnect.handle(this, other)
                    other.onDisconnect.handle(other, this)
                }
            }
            ElectricityManager.refreshNetworks(network, other.network)
        }

        companion object {

            private val CONNECTIONS_KEY = rebarKey("connections")
            private val CONNECTIONS_TYPE = RebarSerializers.SET.setTypeFrom(RebarSerializers.UUID)

            @get:JvmSynthetic
            internal val PDC_TYPE = object : PersistentDataType<PersistentDataContainer, Connector> {
                override fun getPrimitiveType() = PersistentDataContainer::class.java
                override fun getComplexType() = Connector::class.java

                override fun toPrimitive(
                    complex: Connector,
                    context: PersistentDataAdapterContext
                ): PersistentDataContainer {
                    val pdc = context.newPersistentDataContainer()
                    pdc.set(ID_KEY, RebarSerializers.UUID, complex.id)
                    pdc.set(NAME_KEY, RebarSerializers.STRING, complex.name)
                    pdc.set(BLOCK_KEY, RebarSerializers.BLOCK_POSITION, complex.block)
                    pdc.set(CONNECTIONS_KEY, CONNECTIONS_TYPE, complex.connectionSet)
                    return pdc
                }

                override fun fromPrimitive(
                    primitive: PersistentDataContainer,
                    context: PersistentDataAdapterContext
                ): Connector {
                    val id = primitive.get(ID_KEY, RebarSerializers.UUID)!!
                    val name = primitive.get(NAME_KEY, RebarSerializers.STRING)!!
                    val block = primitive.get(BLOCK_KEY, RebarSerializers.BLOCK_POSITION)!!
                    val connectionSet = primitive.get(CONNECTIONS_KEY, CONNECTIONS_TYPE)!!.toMutableSet()
                    return Connector(id, name, block, connectionSet)
                }
            }
        }
    }

    /**
     * Common parent class of non-connector nodes
     */
    @ApiStatus.Internal
    sealed class Leaf(
        id: UUID,
        name: String,
        block: BlockPosition,
        connection: UUID?
    ) : ElectricNode(id, name, block) {

        var connection = connection
            private set

        override fun connect(other: ElectricNode) {
            if (connection != null) throw IllegalStateException("${this::class.simpleName} node is already connected")
            connection = other.id

            when (other) {
                is Leaf -> other.connection = this.id
                is Connector -> other.connectionSet.add(this.id)
            }

            onConnect.handle(this, other)
            other.onConnect.handle(other, this)

            ElectricityManager.mergeNetworks()
        }

        override fun isConnectedTo(other: ElectricNode) = connection == other.id

        override fun disconnectFrom(other: ElectricNode) {
            if (other.id != connection) throw IllegalArgumentException("Not connected to $other")
            disconnect()
        }

        fun disconnect() {
            val otherId = connection ?: return
            connection = null
            val other = ElectricityManager.getNodeById(otherId) ?: throw IllegalStateException("Connected node with ID $otherId not found")
            when (other) {
                is Leaf -> other.connection = null
                is Connector -> other.connectionSet.remove(this.id)
            }

            onDisconnect.handle(this, other)
            other.onDisconnect.handle(other, this)
        }

        companion object {
            @get:JvmSynthetic
            internal val CONNECTION_KEY = rebarKey("connection")
        }
    }

    class Producer private constructor(
        id: UUID,
        name: String,
        block: BlockPosition,
        connection: UUID?,
        var voltage: Double,
        var power: Double
    ) : Leaf(id, name, block, connection) {
        constructor(
            name: String,
            block: BlockPosition,
            voltage: Double,
            power: Double
        ) : this(UUID.randomUUID(), name, block, null, voltage, power)

        @get:JvmSynthetic
        @set:JvmSynthetic
        internal var powerTakeHandler = ConsumerFn<Double> { }

        fun onPowerTake(handler: ConsumerFn<Double>) {
            powerTakeHandler = handler
        }

        companion object {

            private val VOLTAGE_KEY = rebarKey("voltage")
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
                    pdc.set(ID_KEY, RebarSerializers.UUID, complex.id)
                    pdc.set(NAME_KEY, RebarSerializers.STRING, complex.name)
                    pdc.set(BLOCK_KEY, RebarSerializers.BLOCK_POSITION, complex.block)
                    pdc.setNullable(CONNECTION_KEY, RebarSerializers.UUID, complex.connection)
                    pdc.set(VOLTAGE_KEY, RebarSerializers.DOUBLE, complex.voltage)
                    pdc.set(POWER_KEY, RebarSerializers.DOUBLE, complex.power)
                    return pdc
                }

                override fun fromPrimitive(
                    primitive: PersistentDataContainer,
                    context: PersistentDataAdapterContext
                ): Producer {
                    val id = primitive.get(ID_KEY, RebarSerializers.UUID)!!
                    val name = primitive.get(NAME_KEY, RebarSerializers.STRING)!!
                    val block = primitive.get(BLOCK_KEY, RebarSerializers.BLOCK_POSITION)!!
                    val connection = primitive.get(CONNECTION_KEY, RebarSerializers.UUID)
                    val voltage = primitive.get(VOLTAGE_KEY, RebarSerializers.DOUBLE)!!
                    val power = primitive.get(POWER_KEY, RebarSerializers.DOUBLE)!!
                    return Producer(id, name, block, connection, voltage, power)
                }
            }
        }
    }

    class Consumer private constructor(
        id: UUID,
        name: String,
        block: BlockPosition,
        connection: UUID?,
        var voltageRange: VoltageRange,
        var requiredPower: Double
    ) : Leaf(id, name, block, connection) {

        constructor(
            name: String,
            block: BlockPosition,
            voltageRange: VoltageRange,
            requiredPower: Double
        ) : this(UUID.randomUUID(), name, block, null, voltageRange, requiredPower)

        var isPowered: Boolean = false
            @JvmSynthetic
            internal set

        companion object {

            private val VOLTAGE_RANGE_KEY = rebarKey("voltage_range")
            private val VOLTAGE_RANGE_TYPE =
                RebarSerializers.PAIR.pairTypeFrom(RebarSerializers.DOUBLE, RebarSerializers.DOUBLE)
                    .map(
                        from = { VoltageRange(it.first, it.second) },
                        to = { Pair(it.min, it.max) }
                    )
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
                    pdc.set(ID_KEY, RebarSerializers.UUID, complex.id)
                    pdc.set(NAME_KEY, RebarSerializers.STRING, complex.name)
                    pdc.set(BLOCK_KEY, RebarSerializers.BLOCK_POSITION, complex.block)
                    pdc.setNullable(CONNECTION_KEY, RebarSerializers.UUID, complex.connection)
                    pdc.set(VOLTAGE_RANGE_KEY, VOLTAGE_RANGE_TYPE, complex.voltageRange)
                    pdc.set(REQUIRED_POWER_KEY, RebarSerializers.DOUBLE, complex.requiredPower)
                    return pdc
                }

                override fun fromPrimitive(
                    primitive: PersistentDataContainer,
                    context: PersistentDataAdapterContext
                ): Consumer {
                    val id = primitive.get(ID_KEY, RebarSerializers.UUID)!!
                    val name = primitive.get(NAME_KEY, RebarSerializers.STRING)!!
                    val block = primitive.get(BLOCK_KEY, RebarSerializers.BLOCK_POSITION)!!
                    val connection = primitive.get(CONNECTION_KEY, RebarSerializers.UUID)
                    val voltageRange = primitive.get(VOLTAGE_RANGE_KEY, VOLTAGE_RANGE_TYPE)!!
                    val requiredPower = primitive.get(REQUIRED_POWER_KEY, RebarSerializers.DOUBLE)!!
                    return Consumer(id, name, block, connection, voltageRange, requiredPower)
                }
            }
        }
    }

    class Acceptor private constructor(
        id: UUID,
        name: String,
        block: BlockPosition,
        connection: UUID?
    ) : Leaf(id, name, block, connection) {

        constructor(
            name: String,
            block: BlockPosition
        ) : this(UUID.randomUUID(), name, block, null)

        @get:JvmSynthetic
        @set:JvmSynthetic
        internal var handler = AcceptorHandler { 0.0 }

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
                    pdc.set(ID_KEY, RebarSerializers.UUID, complex.id)
                    pdc.set(NAME_KEY, RebarSerializers.STRING, complex.name)
                    pdc.set(BLOCK_KEY, RebarSerializers.BLOCK_POSITION, complex.block)
                    pdc.setNullable(CONNECTION_KEY, RebarSerializers.UUID, complex.connection)
                    return pdc
                }

                override fun fromPrimitive(
                    primitive: PersistentDataContainer,
                    context: PersistentDataAdapterContext
                ): Acceptor {
                    val id = primitive.get(ID_KEY, RebarSerializers.UUID)!!
                    val name = primitive.get(NAME_KEY, RebarSerializers.STRING)!!
                    val block = primitive.get(BLOCK_KEY, RebarSerializers.BLOCK_POSITION)!!
                    val connection = primitive.get(CONNECTION_KEY, RebarSerializers.UUID)
                    return Acceptor(id, name, block, connection)
                }
            }
        }
    }

    companion object {

        @get:JvmSynthetic
        internal val ID_KEY = rebarKey("id")

        @get:JvmSynthetic
        internal val NAME_KEY = rebarKey("name")

        @get:JvmSynthetic
        internal val BLOCK_KEY = rebarKey("block")

        @get:JvmSynthetic
        internal val PDC_TYPE = RebarSerializers.POLYMORPHIC.of(Producer.PDC_TYPE, Consumer.PDC_TYPE, Connector.PDC_TYPE, Acceptor.PDC_TYPE)
    }
}