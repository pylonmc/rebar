package io.github.pylonmc.rebar.electricity

import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.electricity.nodes.ElectricNode
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.entity.RebarEntity
import io.github.pylonmc.rebar.entity.display.ItemDisplayBuilder
import io.github.pylonmc.rebar.entity.display.transform.LineBuilder
import io.github.pylonmc.rebar.entity.interfaces.RemoveRebarEntityHandler
import io.github.pylonmc.rebar.i18n.RebarArgument
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemSchema
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.item.interfaces.WireRebarItem
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.Either
import io.github.pylonmc.rebar.util.minus
import io.github.pylonmc.rebar.util.rebarKey
import net.kyori.adventure.text.Component
import org.bukkit.FluidCollisionMode
import org.bukkit.Location
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityRemoveEvent
import org.joml.Matrix4f
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.ceil
import kotlin.math.min

class WireEntity : RebarEntity<ItemDisplay>, RemoveRebarEntityHandler {

    var port: ConnectedPort
        private set

    var otherEnd: Either<Player, ConnectedPort>
        private set

    var length: Double = 0.0
        private set

    val wireCount: Int get() = wiresRequired(length)

    var wire: RebarItemSchema
        private set

    constructor(port: ConnectedPort, otherEnd: Either<Player, ConnectedPort>, wireItem: WireRebarItem) : super(
        KEY,
        ItemDisplayBuilder()
            .transformation(getTransform(port.location, otherEnd.location))
            .build(port.location)
    ) {
        this.port = port
        this.otherEnd = otherEnd
        this.wire = (wireItem as RebarItem).schema
        this.length = port.location.distance(otherEnd.location)
        EntityStorage.add(this)
        setWireItem(wireItem)
    }

    @Suppress("unused")
    constructor(entity: ItemDisplay) : super(entity) {
        val pdc = entity.persistentDataContainer

        val node = pdc.get(portKey, RebarSerializers.UUID)!!
        val loc1 = pdc.get(portLocKey, RebarSerializers.LOCATION)!!
        port = ConnectedPort(node, loc1)

        val node2 = pdc.get(otherEndKey, RebarSerializers.UUID)!!
        val loc2 = pdc.get(otherEndLocKey, RebarSerializers.LOCATION)!!
        otherEnd = Either.Right(ConnectedPort(node2, loc2))

        wire = pdc.get(itemKey, itemType)!!

        length = loc1.distance(loc2)
    }

    fun setWireItem(wireItem: WireRebarItem) {
        this.wire = (wireItem as RebarItem).schema
        entity.setItemStack(ItemStackBuilder.of(wireItem.displayMaterial).addCustomModelDataString("wire").build())
    }

    val isHeldByPlayer: Boolean get() = otherEnd is Either.Left

    val isObstructed: Boolean get() = isObstructed(port.location, otherEnd.location, length)

    fun canConnect() = canConnect(port.location, otherEnd.location)

    /**
     * Updates the state and visuals of the wire
     */
    fun update() {
        val loc1 = port.location
        val loc2 = otherEnd.location

        entity.setTransformationMatrix(getTransform(loc1, loc2))
        entity.interpolationDelay = 0
        entity.interpolationDuration = 1
        if (entity.location != loc1) {
            entity.teleportAsync(loc1) // in case port was flipped
        }

        length = loc1.distance(loc2)
    }

    /**
     * Makes the player hold the wire entity. Does *not* register the player as connecting in [WireConnectionService].
     * Does nothing if the wire is already being held by a player.
     */
    fun giveToPlayer(player: Player, disconnecting: ElectricNode) {
        val otherEnd = (this.otherEnd as? Either.Right)?.value ?: return
        otherEnd.node.disconnectFrom(port.node)

        if (disconnecting == port.node) {
            port = otherEnd
        }

        this.otherEnd = Either.Left(player)

        update()
    }

    /**
     * Connects the wire between the two ports. Returns the failure reason if failed
     */
    fun connect(port1: ConnectedPort, port2: ConnectedPort): ConnectionFailureReason? {
        val connection = canConnect(port1.location, port2.location)
        if (connection is Either.Right) {
            return connection.value
        }

        (otherEnd as? Either.Right)?.value?.node?.disconnectFrom(port.node)
        port = port1
        otherEnd = Either.Right(port2)

        port1.node.connect(port2.node)

        update()

        return null
    }

    fun dropItemsAt(location: Location) {
        var amount = wireCount
        val item = wire.createNewItemStack()
        while (amount > 0) {
            val toDrop = min(amount, item.maxStackSize)
            amount -= toDrop
            location.world.dropItemNaturally(
                location,
                item.asQuantity(toDrop)
            )
        }
    }

    override fun onUnload() {
        val otherEnd = (this.otherEnd as? Either.Right)?.value

        if (otherEnd != null) {
            val pdc = entity.persistentDataContainer
            pdc.set(portKey, RebarSerializers.UUID, port.node.id)
            pdc.set(portLocKey, RebarSerializers.LOCATION, port.location)

            pdc.set(otherEndKey, RebarSerializers.UUID, otherEnd.node.id)
            pdc.set(otherEndLocKey, RebarSerializers.LOCATION, otherEnd.location)

            pdc.set(itemKey, itemType, wire)
        } else {
            // probably server shutdown
            remove()
        }
    }

    override fun onRemoved(event: EntityRemoveEvent, priority: EventPriority) {
        when (val otherEnd = otherEnd) {
            is Either.Left -> WireConnectionService.stopConnectingWire(otherEnd.value, delete = false)
            is Either.Right -> otherEnd.value.node.disconnectFrom(port.node)
        }
    }

    companion object {

        private val portKey = rebarKey("port")
        private val portLocKey = rebarKey("port_loc")
        private val otherEndKey = rebarKey("other_end")
        private val otherEndLocKey = rebarKey("other_end_loc")
        private val itemKey = rebarKey("item")
        private val itemType = RebarSerializers.KEYED.fromRegistry(RebarRegistry.ITEMS)

        @JvmField
        val KEY = rebarKey("wire")

        const val THICKNESS = 0.05f

        private fun getTransform(start: Location, end: Location): Matrix4f {
            return LineBuilder()
                .from(start.toVector() - start.toVector())
                .to(end.toVector() - start.toVector())
                .thickness(THICKNESS + 0.01f * ThreadLocalRandom.current().nextFloat())
                .build()
                .buildForItemDisplay()
        }

        private fun wiresRequired(length: Double) = ceil(length).toInt()

        private fun isObstructed(start: Location, end: Location, distance: Double) = start.world.rayTraceBlocks(
            start,
            end.toVector() - start.toVector(),
            distance,
            FluidCollisionMode.ALWAYS,
            true
        ) != null

        /**
         * Returns the number of wires needed if a wire could connect between these two locations.
         * Otherwise, returns a [ConnectionFailureReason] detailing why it couldn't connect
         */
        @JvmStatic
        fun canConnect(start: Location, end: Location): Either<Int, ConnectionFailureReason> {
            val dist = start.distance(end)
            return when {
                dist > RebarConfig.WIRING_MAX_LENGTH -> Either.Right(ConnectionFailureReason.TOO_LONG)
                isObstructed(start, end, dist) -> Either.Right(ConnectionFailureReason.OBSTRUCTION)
                else -> Either.Left(wiresRequired(dist))
            }
        }

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        val loadedWires: Collection<WireEntity> get() = EntityStorage.getByKey(KEY) as Collection<WireEntity>
    }

    enum class ConnectionFailureReason(val errorMessage: Component) {
        OBSTRUCTION(Component.translatable("rebar.message.wiring.obstructed")),
        TOO_LONG(Component.translatable("rebar.message.wiring.too_long", RebarArgument.of("blocks", RebarConfig.WIRING_MAX_LENGTH)))
    }

    class ConnectedPort(private val nodeId: UUID, val location: Location) {
        constructor(node: ElectricNode, location: Location) : this(node.id, location)
        val node by lazy { ElectricityManager.getNodeById(nodeId)!! }
    }
}

private val Either<Player, WireEntity.ConnectedPort>.location
    get() = when (this) {
        is Either.Left -> value.eyeLocation.subtract(0.0, 0.5, 0.0)
        is Either.Right -> value.location
    }