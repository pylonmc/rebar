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
import java.util.function.BiConsumer

sealed class ElectricNode(
    val id: UUID,
    val block: BlockPosition
) {

    val network: ElectricNetwork get() = ElectricityManager.getNodeNetwork(this)

    protected var onConnect: BiConsumer<ElectricNode, ElectricNode> = { _, _ -> }
    protected var onDisconnect: BiConsumer<ElectricNode, ElectricNode> = { _, _ -> }

    abstract fun connect(other: ElectricNode)
    abstract fun isConnectedTo(other: ElectricNode): Boolean
    abstract fun disconnectFrom(other: ElectricNode)

    fun onConnect(listener: BiConsumer<ElectricNode, ElectricNode>) {
        val oldOnConnect = onConnect
        onConnect = { n1, n2 ->
            oldOnConnect.accept(n1, n2)
            listener.accept(n1, n2)
        }
    }

    fun onDisconnect(listener: BiConsumer<ElectricNode, ElectricNode>) {
        val oldOnDisconnect = onDisconnect
        onDisconnect = { n1, n2 ->
            oldOnDisconnect.accept(n1, n2)
            listener.accept(n1, n2)
        }
    }

    override fun equals(other: Any?) = other is ElectricNode && id == other.id

    override fun hashCode() = id.hashCode()

    override fun toString(): String {
        return "Electric node of type ${this::class.simpleName} at $block with ID $id"
    }

    class Connector private constructor(
        id: UUID,
        block: BlockPosition,
        @JvmSynthetic internal val connectionSet: MutableSet<UUID>,
    ) : ElectricNode(id, block) {

        constructor(
            block: BlockPosition
        ) : this(UUID.randomUUID(), block, mutableSetOf())

        val connections get() = connectionSet.toSet()

        override fun connect(other: ElectricNode) {
            when (other) {
                is Leaf -> other.connect(this)
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

        override fun disconnectFrom(other: ElectricNode) {
            when (other) {
                is Leaf -> {
                    other.disconnect()
                }

                is Connector -> {
                    connectionSet.remove(other.id)
                    other.connectionSet.remove(this.id)

                    onDisconnect.accept(this, other)
                    other.onDisconnect.accept(other, this)
                }
            }
            ElectricityManager.refreshNetworks(network, other.network)
        }

        companion object {

            private val CONNECTIONS_KEY = rebarKey("connections")
            private val CONNECTIONS_TYPE = RebarSerializers.SET.setTypeFrom(RebarSerializers.UUID)

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
                    pdc.set(CONNECTIONS_KEY, CONNECTIONS_TYPE, complex.connectionSet)
                    return pdc
                }

                override fun fromPrimitive(
                    primitive: PersistentDataContainer,
                    context: PersistentDataAdapterContext
                ): Connector {
                    val id = primitive.get(ID_KEY, RebarSerializers.UUID)!!
                    val block = primitive.get(BLOCK_KEY, RebarSerializers.BLOCK_POSITION)!!
                    val connectionSet = primitive.get(CONNECTIONS_KEY, CONNECTIONS_TYPE)!!.toMutableSet()
                    return Connector(id, block, connectionSet)
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
        block: BlockPosition,
        connection: UUID?
    ) : ElectricNode(id, block) {

        var connection = connection
            private set

        override fun connect(other: ElectricNode) {
            if (connection != null) throw IllegalStateException("${this::class.simpleName} node is already connected")
            connection = other.id

            when (other) {
                is Leaf -> other.connection = this.id
                is Connector -> other.connectionSet.add(this.id)
            }

            onConnect.accept(this, other)
            other.onConnect.accept(other, this)
        }

        override fun isConnectedTo(other: ElectricNode) = connection == other.id

        override fun disconnectFrom(other: ElectricNode) {
            if (other.id != connection) throw IllegalArgumentException("Not connected to $other")
            disconnect()
        }

        fun disconnect() {
            val otherId = connection ?: throw IllegalStateException("${this::class.simpleName} node is not connected")
            connection = null
            val other = ElectricityManager.getNodeById(otherId) ?: throw IllegalStateException("Connected node with ID $otherId not found")
            when (other) {
                is Leaf -> other.connection = null
                is Connector -> other.connectionSet.remove(this.id)
            }

            onDisconnect.accept(this, other)
            other.onDisconnect.accept(other, this)
        }

        companion object {
            @JvmSynthetic
            internal val CONNECTION_KEY = rebarKey("connection")
        }
    }

    class Producer private constructor(
        id: UUID,
        block: BlockPosition,
        connection: UUID?,
        var voltage: Double,
        var power: Double
    ) : Leaf(id, block, connection) {
        constructor(
            block: BlockPosition,
            voltage: Double,
            power: Double
        ) : this(UUID.randomUUID(), block, null, voltage, power)

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
                    val connection = primitive.get(CONNECTION_KEY, RebarSerializers.UUID)
                    val voltage = primitive.get(VOLTAGE_KEY, RebarSerializers.DOUBLE)!!
                    val power = primitive.get(POWER_KEY, RebarSerializers.DOUBLE)!!
                    return Producer(id, block, connection, voltage, power)
                }
            }
        }
    }

    class Consumer private constructor(
        id: UUID,
        block: BlockPosition,
        connection: UUID?,
        var voltageRange: VoltageRange,
        var requiredPower: Double
    ) : Leaf(id, block, connection) {

        constructor(
            block: BlockPosition,
            voltageRange: VoltageRange,
            requiredPower: Double
        ) : this(UUID.randomUUID(), block, null, voltageRange, requiredPower)

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
                    val connection = primitive.get(CONNECTION_KEY, RebarSerializers.UUID)
                    val voltageRange = primitive.get(VOLTAGE_RANGE_KEY, VOLTAGE_RANGE_TYPE)!!
                    val requiredPower = primitive.get(REQUIRED_POWER_KEY, RebarSerializers.DOUBLE)!!
                    return Consumer(id, block, connection, voltageRange, requiredPower)
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
        internal val PDC_TYPE = RebarSerializers.POLYMORPHIC.of(Producer.PDC_TYPE, Consumer.PDC_TYPE, Connector.PDC_TYPE)
    }
}