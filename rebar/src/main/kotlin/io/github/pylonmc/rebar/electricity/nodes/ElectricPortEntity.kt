package io.github.pylonmc.rebar.electricity.nodes

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.electricity.ElectricNetwork
import io.github.pylonmc.rebar.electricity.ElectricityManager
import io.github.pylonmc.rebar.electricity.WireConnectionService
import io.github.pylonmc.rebar.electricity.WireEntity
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.entity.RebarEntity
import io.github.pylonmc.rebar.entity.display.ItemDisplayBuilder
import io.github.pylonmc.rebar.entity.display.transform.TransformBuilder
import io.github.pylonmc.rebar.entity.interfaces.RemoveRebarEntityHandler
import io.github.pylonmc.rebar.event.RebarElectricNodeRemoveEvent
import io.github.pylonmc.rebar.i18n.RebarArgument
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.item.interfaces.WireRebarItem
import io.github.pylonmc.rebar.util.Either
import io.github.pylonmc.rebar.util.rebarKey
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.block.Block
import org.bukkit.entity.ItemDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityRemoveEvent
import org.bukkit.event.player.PlayerInteractEvent
import kotlin.math.PI

class ElectricPortEntity : RebarEntity<ItemDisplay>, RemoveRebarEntityHandler, Listener {

    val node: ElectricNode by lazy { ElectricityManager.getNodeById(entity.persistentDataContainer.get(nodeKey, RebarSerializers.UUID)!!)!! }

    constructor(block: Block, port: ElectricPortSpec) : super(
        KEY,
        ItemDisplayBuilder()
            .itemStack(ItemStackBuilder.of(port.material).addCustomModelDataString("electric_port"))
            .transformation(
                TransformBuilder()
                    .rotate(port.face.direction.toVector3d(), PI / 4)
                    // why all the math? well the port itself needs to exist slightly outside the radius
                    // to get proper lighting, so we spawn it there and offset it back.
                    // everything after the - is just offsetting it back further so it won't stick out
                    .translate(
                        port.face.direction.multiply(port.radius * -0.01 - SCALE / 2 * 0.99).add(port.offset)
                            .toVector3d()
                    )
                    .scale(SCALE)
            )
            .build(block.location.toCenterLocation().add(port.face.direction.multiply(port.radius * 1.01)))
    ) {
        entity.persistentDataContainer.set(nodeKey, RebarSerializers.UUID, port.node.id)

        EntityStorage.add(this)
    }

    @Suppress("unused")
    constructor(entity: ItemDisplay) : super(entity)

    init {
        Bukkit.getPluginManager().registerEvents(this, Rebar)
    }

    val connectedWires: List<WireEntity>
        get() = WireEntity.loadedWires.filter { wire -> wire.port.node == node || (wire.otherEnd as? Either.Right)?.value?.node == node }

    private fun dropConnectedWires() {
        for (wire in connectedWires) {
            if (wire.isHeldByPlayer) continue
            wire.dropItemsAt(entity.location)
            wire.remove()
        }
    }

    @EventHandler
    private fun onNodeRemove(event: RebarElectricNodeRemoveEvent) {
        if (event.node == node) {
            dropConnectedWires()
        }
    }

    override fun onRemoved(event: EntityRemoveEvent, priority: EventPriority) {
        HandlerList.unregisterAll(this)
    }

    fun onInteractedWith(event: PlayerInteractEvent) {
        if (event.action.isRightClick) {
            handleWireConnection(event)
        } else if (RebarItem.isRebarItem<WireRebarItem>(event.player.inventory.itemInMainHand)) {
            dropConnectedWires()
        }
    }

    private fun handleWireConnection(event: PlayerInteractEvent) {
        val player = event.player
        val wire = WireConnectionService.getWirePlayerIsConnecting(player)

        if (wire == null) {
            val wireItem = RebarItem.fromStack<WireRebarItem>(player.inventory.itemInMainHand) ?: return
            val wire = WireEntity(WireEntity.ConnectedPort(node, entity.location), Either.Left(player), wireItem)
            wire.giveToPlayer(player, node)
            WireConnectionService.startConnectingWire(player, wire)
        } else if (wire.port.node == node || wire.port.node.isConnectedTo(node)) {
            WireConnectionService.stopConnectingWire(player)
        } else {
            val otherPort = wire.port
            val wires = when (val connection = WireEntity.canConnect(otherPort.location, entity.location)) {
                is Either.Left -> connection.value
                is Either.Right -> {
                    player.sendMessage(connection.value.errorMessage)
                    return
                }
            }

            val mainHandItem = player.inventory.itemInMainHand
            if (wires > mainHandItem.amount && player.gameMode != GameMode.CREATIVE) {
                player.sendMessage(
                    Component.translatable(
                        "rebar.message.wiring.more_wires",
                        RebarArgument.of("wires", mainHandItem.amount),
                        RebarArgument.of("total", wires)
                    )
                )
                return
            }

            WireConnectionService.stopConnectingWire(player, delete = false)
            val wireItem = RebarItem.fromStack<WireRebarItem>(mainHandItem)!!

            wire.setWireItem(wireItem)
            wire.connect(otherPort, WireEntity.ConnectedPort(node, entity.location))

            ElectricNetwork.Edge(wire.port.node, node).powerLimit = wireItem.maxPower
            ElectricNetwork.Edge(node, wire.port.node).powerLimit = wireItem.maxPower

            if (player.gameMode != GameMode.CREATIVE) {
                player.inventory.setItemInMainHand(mainHandItem.subtract(wires))
            }
        }
    }

    companion object {
        private val nodeKey = rebarKey("node")

        @JvmField
        val KEY = rebarKey("electric_port")

        const val SCALE = 0.19
    }
}