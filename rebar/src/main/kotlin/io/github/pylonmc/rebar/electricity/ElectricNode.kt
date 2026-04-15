package io.github.pylonmc.rebar.electricity

import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.datatypes.map
import io.github.pylonmc.rebar.util.position.BlockPosition
import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.util.setNullable
import org.bukkit.Location
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.jetbrains.annotations.ApiStatus
import java.util.*
import java.util.function.BiConsumer

sealed class ElectricNode(
    val id: UUID,
    val block: BlockPosition,
    connectionPoint: Location
) {

    // screw bukkit for making Location mutable
    val connectionPoint: Location = connectionPoint.clone()
        get() = field.clone()

    val network: ElectricNetwork get() = ElectricityManager.getNodeNetwork(this)

    abstract fun isConnectedTo(other: ElectricNode): Boolean

    override fun equals(other: Any?) = other is ElectricNode && id == other.id

    override fun hashCode() = id.hashCode()

    override fun toString(): String {
        return "Electric node of type ${this::class.simpleName} at $block with ID $id"
    }

    class Connector private constructor(
        id: UUID,
        block: BlockPosition,
        connectionPoint: Location,
        @JvmSynthetic internal val connectionSet: MutableSet<UUID>,
        private val currentLimits: MutableMap<UUID, Double>,
    ) : ElectricNode(id, block, connectionPoint) {

        constructor(
            block: BlockPosition,
            connectionPoint: Location
        ) : this(UUID.randomUUID(), block, connectionPoint, mutableSetOf(), mutableMapOf())

        val connections get() = connectionSet.toSet()

        fun connect(other: ElectricNode) {
            when (other) {
                is Leaf<*> -> other.connect(this)
                is Connector -> {
                    connectionSet.add(other.id)
                    other.connectionSet.add(this.id)

                    onConnect.accept(this, other)
                    other.onConnect.accept(other, this)
                }
            }
            ElectricityManager.mergeNetworks()
        }

        override fun isConnectedTo(other: ElectricNode) = other.id in connectionSet

        fun disconnect(other: ElectricNode) {
            when (other) {
                is Leaf<*> -> {
                    other.disconnect()
                    currentLimits.remove(other.id)
                }

                is Connector -> {
                    connectionSet.remove(other.id)
                    other.connectionSet.remove(this.id)
                    currentLimits.remove(other.id)
                    other.currentLimits.remove(this.id)

                    onDisconnect.accept(this, other)
                    other.onDisconnect.accept(other, this)
                }
            }
            ElectricityManager.refreshNetworks(network, other.network)
        }

        @JvmSynthetic
        internal var onConnect: BiConsumer<Connector, ElectricNode> = BiConsumer { _, _ -> }

        @JvmSynthetic
        internal var onDisconnect: BiConsumer<Connector, ElectricNode> = BiConsumer { _, _ -> }

        fun onConnect(listener: BiConsumer<Connector, ElectricNode>) {
            onConnect = listener
        }

        fun onDisconnect(listener: BiConsumer<Connector, ElectricNode>) {
            onDisconnect = listener
        }

        fun setCurrentLimit(other: ElectricNode, limit: Double) {
            if (!isConnectedTo(other)) throw IllegalArgumentException("Not connected to $other")
            currentLimits[other.id] = limit
        }

        fun getCurrentLimit(other: ElectricNode): Double {
            if (!isConnectedTo(other)) throw IllegalArgumentException("Not connected to $other")
            return currentLimits[other.id] ?: Double.MAX_VALUE
        }

        companion object {

            private val CONNECTIONS_KEY = rebarKey("connections")
            private val CONNECTIONS_TYPE = RebarSerializers.SET.setTypeFrom(RebarSerializers.UUID)
            private val CURRENT_LIMITS_KEY = rebarKey("current_limits")
            private val CURRENT_LIMITS_TYPE = RebarSerializers.MAP.mapTypeFrom(RebarSerializers.UUID, RebarSerializers.DOUBLE)

            @JvmSynthetic
            internal val PDC_TYPE = object : PersistentDataType<PersistentDataContainer, Connector> {
                override fun getPrimitiveType() = PersistentDataContainer::class.java
                override fun getComplexType() = Connector::class.java

                override fun toPrimitive(
                    complex: Connector,
                    context: PersistentDataAdapterContext
                ): PersistentDataContainer {
                    val pdc = context.newPersistentDataContainer()
                    pdc.set(ID_KEY, RebarSerializers.UUID, complex.id)
                    pdc.set(BLOCK_KEY, RebarSerializers.BLOCK_POSITION, complex.block)
                    pdc.set(CONNECTION_POINT_KEY, RebarSerializers.LOCATION, complex.connectionPoint)
                    pdc.set(CONNECTIONS_KEY, CONNECTIONS_TYPE, complex.connectionSet)
                    pdc.set(CURRENT_LIMITS_KEY, CURRENT_LIMITS_TYPE, complex.currentLimits)
                    return pdc
                }

                override fun fromPrimitive(
                    primitive: PersistentDataContainer,
                    context: PersistentDataAdapterContext
                ): Connector {
                    val id = primitive.get(ID_KEY, RebarSerializers.UUID)!!
                    val block = primitive.get(BLOCK_KEY, RebarSerializers.BLOCK_POSITION)!!
                    val connectionPoint = primitive.get(CONNECTION_POINT_KEY, RebarSerializers.LOCATION)!!
                    val connectionSet = primitive.get(CONNECTIONS_KEY, CONNECTIONS_TYPE)!!.toMutableSet()
                    val currentLimits = primitive.get(CURRENT_LIMITS_KEY, CURRENT_LIMITS_TYPE)!!.toMutableMap()
                    return Connector(id, block, connectionPoint, connectionSet, currentLimits)
                }
            }
        }
    }

    /**
     * Common parent class of non-connector nodes
     *
     * @param S the specific type of the leaf node, used for connect/disconnect listeners
     */
    @ApiStatus.Internal
    sealed class Leaf<S : Leaf<S>>(
        id: UUID,
        block: BlockPosition,
        connectionPoint: Location,
        connection: UUID?
    ) : ElectricNode(id, block, connectionPoint) {

        var connection = connection
            private set

        fun connect(other: Connector) {
            if (connection != null) throw IllegalStateException("${this::class.simpleName} node is already connected")
            connection = other.id
            other.connectionSet.add(this.id)

            @Suppress("UNCHECKED_CAST")
            onConnect.accept(this as S, other)
            other.onConnect.accept(other, this)
        }

        override fun isConnectedTo(other: ElectricNode) = connection == other.id

        fun disconnect() {
            val otherId = connection ?: throw IllegalStateException("${this::class.simpleName} node is not connected")
            connection = null
            val other = ElectricityManager.getNodeById(otherId) as Connector
            other.connectionSet.remove(this.id)

            @Suppress("UNCHECKED_CAST")
            onDisconnect.accept(this as S, other)
            other.onDisconnect.accept(other, this)
        }

        private var onConnect: BiConsumer<S, Connector> = BiConsumer { _, _ -> }
        private var onDisconnect: BiConsumer<S, Connector> = BiConsumer { _, _ -> }

        fun onConnect(listener: BiConsumer<S, Connector>) {
            onConnect = listener
        }

        fun onDisconnect(listener: BiConsumer<S, Connector>) {
            onDisconnect = listener
        }

        companion object {
            @JvmSynthetic
            internal val CONNECTION_KEY = rebarKey("connection")
        }
    }

    class Producer private constructor(
        id: UUID,
        block: BlockPosition,
        connectionPoint: Location,
        connection: UUID?,
        var voltage: Double,
        var power: Double
    ) : Leaf<Producer>(id, block, connectionPoint, connection) {
        constructor(
            block: BlockPosition,
            connectionPoint: Location,
            voltage: Double,
            power: Double
        ) : this(UUID.randomUUID(), block, connectionPoint, null, voltage, power)

        companion object {

            private val VOLTAGE_KEY = rebarKey("voltage")
            private val POWER_KEY = rebarKey("power")

            @JvmSynthetic
            internal val PDC_TYPE = object : PersistentDataType<PersistentDataContainer, Producer> {
                override fun getPrimitiveType() = PersistentDataContainer::class.java
                override fun getComplexType() = Producer::class.java

                override fun toPrimitive(
                    complex: Producer,
                    context: PersistentDataAdapterContext
                ): PersistentDataContainer {
                    val pdc = context.newPersistentDataContainer()
                    pdc.set(ID_KEY, RebarSerializers.UUID, complex.id)
                    pdc.set(BLOCK_KEY, RebarSerializers.BLOCK_POSITION, complex.block)
                    pdc.set(CONNECTION_POINT_KEY, RebarSerializers.LOCATION, complex.connectionPoint)
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
                    val block = primitive.get(BLOCK_KEY, RebarSerializers.BLOCK_POSITION)!!
                    val connectionPoint = primitive.get(CONNECTION_POINT_KEY, RebarSerializers.LOCATION)!!
                    val connection = primitive.get(CONNECTION_KEY, RebarSerializers.UUID)
                    val voltage = primitive.get(VOLTAGE_KEY, RebarSerializers.DOUBLE)!!
                    val power = primitive.get(POWER_KEY, RebarSerializers.DOUBLE)!!
                    return Producer(id, block, connectionPoint, connection, voltage, power)
                }
            }
        }
    }

    class Consumer private constructor(
        id: UUID,
        block: BlockPosition,
        connectionPoint: Location,
        connection: UUID?,
        var voltageRange: VoltageRange,
        var requiredPower: Double
    ) : Leaf<Consumer>(id, block, connectionPoint, connection) {

        constructor(
            block: BlockPosition,
            connectionPoint: Location,
            voltageRange: VoltageRange,
            requiredPower: Double
        ) : this(UUID.randomUUID(), block, connectionPoint, null, voltageRange, requiredPower)

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

            @JvmSynthetic
            internal val PDC_TYPE = object : PersistentDataType<PersistentDataContainer, Consumer> {
                override fun getPrimitiveType() = PersistentDataContainer::class.java
                override fun getComplexType() = Consumer::class.java

                override fun toPrimitive(
                    complex: Consumer,
                    context: PersistentDataAdapterContext
                ): PersistentDataContainer {
                    val pdc = context.newPersistentDataContainer()
                    pdc.set(ID_KEY, RebarSerializers.UUID, complex.id)
                    pdc.set(BLOCK_KEY, RebarSerializers.BLOCK_POSITION, complex.block)
                    pdc.set(CONNECTION_POINT_KEY, RebarSerializers.LOCATION, complex.connectionPoint)
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
                    val block = primitive.get(BLOCK_KEY, RebarSerializers.BLOCK_POSITION)!!
                    val connectionPoint = primitive.get(CONNECTION_POINT_KEY, RebarSerializers.LOCATION)!!
                    val connection = primitive.get(CONNECTION_KEY, RebarSerializers.UUID)
                    val voltageRange = primitive.get(VOLTAGE_RANGE_KEY, VOLTAGE_RANGE_TYPE)!!
                    val requiredPower = primitive.get(REQUIRED_POWER_KEY, RebarSerializers.DOUBLE)!!
                    return Consumer(id, block, connectionPoint, connection, voltageRange, requiredPower)
                }
            }
        }
    }

    companion object {

        @JvmSynthetic
        internal val ID_KEY = rebarKey("id")

        @JvmSynthetic
        internal val BLOCK_KEY = rebarKey("block")

        @JvmSynthetic
        internal val CONNECTION_POINT_KEY = rebarKey("connection_point")

        @JvmSynthetic
        internal val PDC_TYPE = RebarSerializers.POLYMORPHIC.of(Producer.PDC_TYPE, Consumer.PDC_TYPE, Connector.PDC_TYPE)
    }
}