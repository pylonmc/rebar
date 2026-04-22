package io.github.pylonmc.rebar.electricity

import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.block.base.RebarElectricBlock
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.entity.display.ItemDisplayBuilder
import io.github.pylonmc.rebar.entity.display.transform.TransformBuilder
import io.github.pylonmc.rebar.event.RebarBlockBreakEvent
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.base.RebarWire
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.util.rebarKey
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.FluidCollisionMode
import org.bukkit.Location
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.*
import org.joml.Matrix4f
import java.util.*

internal object WireConnectionService : Listener {

    private val interactions = WeakHashMap<Interaction, ElectricNode>()
    private val locations = WeakHashMap<ElectricNode, Location>()
    private val CONNECTING_KEY = rebarKey("connecting")
    private val CONNECTING_NODE_KEY = rebarKey("connecting_node")

    fun addInteraction(interaction: Interaction, node: ElectricNode) {
        interactions[interaction] = node
        locations[node] = interaction.location.add(0.0, interaction.height / 2, 0.0)
        node.onDisconnect { thisNode, otherNode ->
            val block = BlockStorage.getAs<RebarElectricBlock>(thisNode.block) ?: return@onDisconnect
            val connectionName = getConnectionName(thisNode, otherNode)
            block.tryRemoveEntity(connectionName)
        }
    }

    @EventHandler(ignoreCancelled = true)
    private fun onInteract(event: PlayerInteractEntityEvent) {
        val thisNode = interactions[event.rightClicked] ?: return
        val thisLocation = locations[thisNode] ?: return
        val thisBlock = BlockStorage.getAsOrThrow<RebarElectricBlock>(thisNode.block)
        val player = event.player
        val playerPdc = player.persistentDataContainer

        val connectingEntity = player.connectingEntity
        if (connectingEntity == null) {
            val existingConnection = when (thisNode) {
                is ElectricNode.Leaf -> thisNode.connection
                is ElectricNode.Connector -> thisNode.connections.firstOrNull {
                    val node = ElectricityManager.getNodeById(it) ?: return@firstOrNull false
                    thisBlock.isHeldEntityPresent(getConnectionName(thisNode, node))
                }
            }?.let(ElectricityManager::getNodeById)

            val playerLocation = player.eyeLocation.subtract(0.0, 0.5, 0.0)
            val (displayMaterial, node) = if (existingConnection != null) {
                val wire = thisBlock.getHeldEntityOrThrow(getConnectionName(thisNode, existingConnection)) as ItemDisplay
                val material = wire.itemStack.type
                thisNode.disconnectFrom(existingConnection)
                // TODO refund wires
                material to existingConnection
            } else {
                val itemInHand = player.inventory.getItem(event.hand)
                val wire = RebarItem.from<RebarWire>(itemInHand) ?: return
                wire.displayMaterial to thisNode
            }
            val otherEnd = locations[node] ?: return
            val display = ItemDisplayBuilder()
                .transformation(getDisplayTransform(otherEnd, playerLocation))
                .material(displayMaterial)
                .build(getMidpoint(otherEnd, playerLocation))
            display.persistentDataContainer.set(CONNECTING_NODE_KEY, RebarSerializers.UUID, node.id)
            playerPdc.set(CONNECTING_KEY, RebarSerializers.UUID, display.uniqueId)
            checkCanRunWire(player, node, playerLocation)
            event.isCancelled = true
            return
        }

        event.isCancelled = true

        val connectingNodeId =
            connectingEntity.persistentDataContainer.get(CONNECTING_NODE_KEY, RebarSerializers.UUID) ?: return
        val connectingNode = ElectricityManager.getNodeById(connectingNodeId) ?: return
        if (connectingNode == thisNode) {
            deleteConnecting(player)
            return
        }

        if (connectingNode.isConnectedTo(thisNode)) {
            deleteConnecting(player)
            connectingNode.disconnectFrom(thisNode)
            return
        }

        if (!checkCanRunWire(player, connectingNode, thisLocation)) return
        player.sendActionBar(Component.empty())

        if (thisNode is ElectricNode.Leaf && thisNode.connection != null) {
            player.sendMessage(Component.translatable("rebar.message.electricity.already_connected"))
            return
        }

        val wire = RebarItem.from<RebarWire>(player.inventory.getItem(event.hand)) ?: return
        connectingNode.connect(thisNode)
        ElectricityManager.setMaxCurrent(thisNode, connectingNode, wire.maxCurrent)
        val connectingLocation = locations[connectingNode] ?: return
        connectingEntity.setTransformationMatrix(getDisplayTransform(connectingLocation, thisLocation))
        connectingEntity.teleportAsync(getMidpoint(connectingLocation, thisLocation))

        val connectingBlock = BlockStorage.getAsOrThrow<RebarElectricBlock>(connectingNode.block)
        thisBlock.addEntity(getConnectionName(thisNode, connectingNode), connectingEntity)
        connectingBlock.addEntity(getConnectionName(connectingNode, thisNode), connectingEntity)

        player.persistentDataContainer.remove(CONNECTING_KEY)
    }

    private fun getConnectionName(from: ElectricNode, to: ElectricNode): String {
        return "connection_${from.id}->${to.id}"
    }

    private fun checkCanRunWire(
        player: Player,
        connectingFromNode: ElectricNode,
        connectionLocation: Location
    ): Boolean {
        val connectingFromLocation = locations[connectingFromNode] ?: return false
        val direction = connectionLocation.toVector().subtract(connectingFromLocation.toVector()).normalize()
        val startLocation = connectingFromLocation.clone().add(direction.multiply(0.1))
        val endLocation = connectionLocation.clone().subtract(direction.multiply(0.1))
        val rayTraceResult = startLocation.world.rayTraceBlocks(
            startLocation,
            direction,
            startLocation.distance(endLocation),
            FluidCollisionMode.ALWAYS,
            true
        )
        if (rayTraceResult != null) {
            player.sendActionBar(Component.translatable("rebar.message.electricity.blocking"))
            return false
        } else {
            player.sendActionBar(Component.translatable("rebar.message.electricity.default"))
            return true
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    private fun onElectricBlockBreak(event: RebarBlockBreakEvent) {
        val block = event.block as? RebarElectricBlock ?: return
        for (player in Bukkit.getOnlinePlayers()) {
            val connectingEntity = player.connectingEntity ?: continue
            val nodeId =
                connectingEntity.persistentDataContainer.get(CONNECTING_NODE_KEY, RebarSerializers.UUID) ?: continue
            val node = ElectricityManager.getNodeById(nodeId) ?: continue
            if (node in block.electricNodes) {
                deleteConnecting(player)
            }
        }
    }

    @EventHandler
    private fun onPlayerMove(event: PlayerMoveEvent) {
        if (!event.hasChangedPosition()) return
        val player = event.player
        val connectingEntity = player.connectingEntity ?: return
        val connectingNode = connectingEntity.persistentDataContainer.get(CONNECTING_NODE_KEY, RebarSerializers.UUID)?.let(ElectricityManager::getNodeById) ?: return
        val connectingLocation = locations[connectingNode] ?: return
        val playerLocation = player.eyeLocation.subtract(0.0, 0.5, 0.0)
        connectingEntity.teleportDuration = 1
        connectingEntity.interpolationDelay = 0
        connectingEntity.interpolationDuration = 1
        connectingEntity.setTransformationMatrix(getDisplayTransform(connectingLocation, playerLocation))
        connectingEntity.teleportAsync(getMidpoint(connectingLocation, playerLocation))
        checkCanRunWire(player, connectingNode, playerLocation)
    }

    @EventHandler
    private fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        if (!player.persistentDataContainer.has(CONNECTING_KEY)) return
        deleteConnecting(player)
    }

    @EventHandler
    private fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        if (!player.persistentDataContainer.has(CONNECTING_KEY)) return
        deleteConnecting(player)
    }

    @EventHandler
    private fun onPlayerScroll(event: PlayerItemHeldEvent) {
        val player = event.player
        if (!player.persistentDataContainer.has(CONNECTING_KEY)) return
        val item = player.inventory.getItem(event.newSlot)?.let(RebarItem::fromStack)
        if (item !is RebarWire) {
            deleteConnecting(player)
        } else {
            updateWireMaterial(player, item)
        }
    }

    @EventHandler
    private fun onPlayerSwap(event: PlayerSwapHandItemsEvent) {
        val player = event.player
        if (!player.persistentDataContainer.has(CONNECTING_KEY)) return
        val item = RebarItem.fromStack(event.mainHandItem)
        if (item !is RebarWire) {
            deleteConnecting(player)
        } else {
            updateWireMaterial(player, item)
        }
    }

    private fun deleteConnecting(player: Player) {
        player.connectingEntity?.remove()
        player.persistentDataContainer.remove(CONNECTING_KEY)
        player.sendActionBar(Component.empty())
    }

    private fun updateWireMaterial(player: Player, item: RebarWire) {
        val connectingEntity = player.connectingEntity ?: return
        val material = item.displayMaterial
        connectingEntity.setItemStack(ItemStackBuilder.of(material).addCustomModelDataString("wire").build())
    }

    private val Player.connectingEntity: ItemDisplay?
        get() {
            val connectingEntityId = persistentDataContainer.get(CONNECTING_KEY, RebarSerializers.UUID) ?: return null
            return Bukkit.getEntity(connectingEntityId) as? ItemDisplay
        }

    private fun getDisplayTransform(from: Location, to: Location): Matrix4f {
        return TransformBuilder()
            .lookAlong(from, to)
            .scale(0.05, 0.05, from.distance(to))
            .buildForItemDisplay()
    }

    private fun getMidpoint(a: Location, b: Location): Location {
        return a.clone().add(b).multiply(0.5)
    }
}