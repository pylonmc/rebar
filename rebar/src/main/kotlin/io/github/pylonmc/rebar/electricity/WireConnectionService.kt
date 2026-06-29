package io.github.pylonmc.rebar.electricity

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.block.interfaces.ElectricRebarBlock
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.electricity.nodes.ElectricNode
import io.github.pylonmc.rebar.entity.display.ItemDisplayBuilder
import io.github.pylonmc.rebar.entity.display.transform.LineBuilder
import io.github.pylonmc.rebar.event.RebarBlockBreakEvent
import io.github.pylonmc.rebar.i18n.RebarArgument
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.item.interfaces.WireRebarItem
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.minus
import io.github.pylonmc.rebar.util.rebarKey
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.FluidCollisionMode
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.joml.Matrix4f
import java.util.*
import kotlin.math.ceil
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds

internal object WireConnectionService : Listener {

    private val interactions = WeakHashMap<Interaction, ElectricNode>()
    private val locations = WeakHashMap<ElectricNode, Location>()
    private val CONNECTING_KEY = rebarKey("connecting")
    private val CONNECTING_NODE_KEY = rebarKey("connecting_node")
    private val WIRE_TYPE_KEY = rebarKey("wire_type")

    private val messageTasks = mutableMapOf<UUID, Job>()

    fun addInteraction(interaction: Interaction, node: ElectricNode) {
        interactions[interaction] = node
        locations[node] = interaction.location.add(0.0, interaction.height / 2, 0.0)
        node.onDisconnect { thisNode, otherNode ->
            val block = BlockStorage.getAs<ElectricRebarBlock>(thisNode.block) ?: return@onDisconnect
            val connectionName = getConnectionName(thisNode, otherNode)
            block.tryRemoveEntity(connectionName)
        }
    }

    @EventHandler(ignoreCancelled = true)
    private fun onInteract(event: PlayerInteractEntityEvent) {
        val thisNode = interactions[event.rightClicked] ?: return
        val thisLocation = locations[thisNode] ?: return
        val thisBlock = BlockStorage.getAsOrThrow<ElectricRebarBlock>(thisNode.block)
        val player = event.player
        val playerInv = player.inventory

        val connectingEntity = player.connectingEntity
        if (connectingEntity == null) {
            // Player is not currently connecting any wires
            if (startConnection(player, event.hand, thisNode, thisBlock)) {
                event.isCancelled = true
                player.swingHand(event.hand)
            }
            return
        }

        // Player has right clicked a port while connecting a wire, so we will attempt to connect the wire to this port
        event.isCancelled = true
        player.swingHand(event.hand)

        val connectingNodeId =
            connectingEntity.persistentDataContainer.get(CONNECTING_NODE_KEY, RebarSerializers.UUID) ?: return

        // The node the wire is connected to
        val connectingNode = ElectricityManager.getNodeById(connectingNodeId) ?: return
        if (connectingNode == thisNode) {
            // Player has right clicked the same port they started connecting from, so we will cancel the connection
            deleteConnecting(player)
            return
        }

        if (connectingNode.isConnectedTo(thisNode)) {
            // If the two ports are already connected, we will disconnect them instead of connecting them
            deleteConnecting(player)
            connectingNode.disconnectFrom(thisNode)
            return
        }

        if (!checkCanRunWire(player, connectingNode, thisLocation)) return
        player.sendActionBar(Component.empty())

        val wireItem = playerInv.getItem(event.hand)
        val wire = RebarItem.from<WireRebarItem>(wireItem) ?: return
        connectingNode.connect(thisNode)

        ElectricNetwork.Edge(thisNode, connectingNode).powerLimit = wire.maxPower
        ElectricNetwork.Edge(connectingNode, thisNode).powerLimit = wire.maxPower

        // Move it so that it'll connect the two ports
        val connectingLocation = locations[connectingNode] ?: return
        connectingEntity.setTransformationMatrix(getDisplayTransform(connectingLocation, thisLocation, connectingEntity.location))

        // Kinda cursed solution: make both blocks own the same entity so that it will be removed when either block is broken
        val connectingBlock = BlockStorage.getAsOrThrow<ElectricRebarBlock>(connectingNode.block)
        thisBlock.addEntity(getConnectionName(thisNode, connectingNode), connectingEntity)
        connectingBlock.addEntity(getConnectionName(connectingNode, thisNode), connectingEntity)

        connectingEntity.persistentDataContainer.set(
            WIRE_TYPE_KEY,
            RebarSerializers.NAMESPACED_KEY,
            (wire as RebarItem).key
        )
        player.persistentDataContainer.remove(CONNECTING_KEY)

        if (player.gameMode != GameMode.CREATIVE) {
            // Remove the wire items from the player's inventory
            var remaining = ceil(connectingLocation.distance(thisLocation)).toInt()
            while (remaining > 0) {
                for (i in playerInv.contents.indices) {
                    val item = playerInv.getItem(i) ?: continue
                    if (!item.isSimilar(wireItem)) continue
                    val toRemove = minOf(item.amount, remaining)
                    item.amount -= toRemove
                    remaining -= toRemove
                    if (remaining <= 0) break
                }
            }
        }
    }

    private fun startConnection(player: Player, hand: EquipmentSlot, thisNode: ElectricNode, thisBlock: ElectricRebarBlock): Boolean {
        val playerInv = player.inventory
        val thisLocation = locations[thisNode]!!

        val existingConnection = thisNode.connections.firstOrNull {
            val node = ElectricityManager.getNodeById(it) ?: return@firstOrNull false
            thisBlock.isHeldEntityPresent(getConnectionName(thisNode, node))
        }?.let(ElectricityManager::getNodeById)

        val playerLocation = player.eyeLocation.subtract(0.0, 0.5, 0.0)
        val (wireItem, node) = if (existingConnection != null) {
            // Clicked a node that is already connected to another node, so we will disconnect it and give the player the wire items back
            val wireEntity =
                thisBlock.getHeldEntityOrThrow(getConnectionName(thisNode, existingConnection)) as ItemDisplay
            val wireType = RebarRegistry.ITEMS.getOrThrow(
                wireEntity.persistentDataContainer.get(
                    WIRE_TYPE_KEY,
                    RebarSerializers.NAMESPACED_KEY
                )!!
            )
            val wireItem = wireType.createNewItemStack()

            if (player.gameMode != GameMode.CREATIVE) {
                // Give the player the wire items back, dropping any excess on the ground
                val mainHandItem = playerInv.itemInMainHand

                var amount = ceil(locations[existingConnection]!!.distance(thisLocation)).toInt()
                val takenAmount = min(amount, wireItem.maxStackSize)
                playerInv.setItemInMainHand(wireItem.asQuantity(takenAmount))
                amount -= takenAmount

                val noFit = mutableListOf<ItemStack>()
                while (amount > 0) {
                    val takenAmount = min(amount, wireItem.maxStackSize)
                    val excess = playerInv.addItem(wireItem.asQuantity(takenAmount))
                    noFit.addAll(excess.values)
                    amount -= takenAmount
                }
                noFit.addAll(playerInv.addItem(mainHandItem).values)

                for (item in noFit) {
                    player.world.dropItem(player.location, item)
                }
            }

            thisNode.disconnectFrom(existingConnection)
            wireItem to existingConnection
        } else {
            // Clicked a node that is not connected to another node, so we will start a new connection
            val handItem = playerInv.getItem(hand)
            if (!RebarItem.isRebarItem<WireRebarItem>(handItem)) {
                player.sendMessage(Component.translatable("rebar.message.electricity.need_wire"))
                return false
            }
            handItem to thisNode
        }

        val wire = RebarItem.from<WireRebarItem>(wireItem) ?: return false
        val otherEnd = locations[node] ?: return false
        val display = ItemDisplayBuilder()
            .transformation(getDisplayTransform(otherEnd, playerLocation, otherEnd))
            .material(wire.displayMaterial)
            .build(otherEnd)
        display.persistentDataContainer.set(CONNECTING_NODE_KEY, RebarSerializers.UUID, node.id)
        player.persistentDataContainer.set(CONNECTING_KEY, RebarSerializers.UUID, display.uniqueId)
        messageTasks[player.uniqueId] = Rebar.scope.launch {
            while (true) {
                checkCanRunWire(player, node, player.location)
                delay(1.seconds) // resend every second no matter when the player moves
            }
        }
        return true
    }

    private fun getConnectionName(from: ElectricNode, to: ElectricNode): String {
        return "connection_${from.id}->${to.id}"
    }

    /**
     * Returns true if there are no blocks between the two locations, and the player has enough wire items to connect them.
     * If there are blocks in the way, or the player does not have enough wire items, a message will be sent to the player.
     */
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
        }

        val wireItem = player.inventory.itemInMainHand
        check(RebarItem.fromStack(wireItem) is WireRebarItem) { "Held item must be a wire" }
        val totalWires = player.inventory.sumOf { if (it != null && it.isSimilar(wireItem)) it.amount else 0 }
        val neededWires = ceil(connectingFromLocation.distance(connectionLocation)).toInt()
        val hasEnough = neededWires <= totalWires || player.gameMode == GameMode.CREATIVE
        player.sendActionBar(
            Component.text()
                .color(if (hasEnough) NamedTextColor.WHITE else NamedTextColor.RED)
                .append(
                    Component.translatable(
                        "rebar.message.electricity.default",
                        RebarArgument.of("wires", neededWires),
                        RebarArgument.of("total", totalWires)
                    )
                )
        )

        return hasEnough
    }

    private fun deleteConnecting(player: Player) {
        player.connectingEntity?.remove()
        player.persistentDataContainer.remove(CONNECTING_KEY)
        player.sendActionBar(Component.empty())
        messageTasks.remove(player.uniqueId)?.cancel()
    }

    private fun updateWireMaterial(player: Player, item: WireRebarItem) {
        val connectingEntity = player.connectingEntity ?: return
        val material = item.displayMaterial
        connectingEntity.setItemStack(ItemStackBuilder.of(material).addCustomModelDataString("wire").build())
    }

    /**
     * The wire display entities a player is currently connecting
     */
    private val Player.connectingEntity: ItemDisplay?
        get() {
            val connectingEntityId = persistentDataContainer.get(CONNECTING_KEY, RebarSerializers.UUID) ?: return null
            return Bukkit.getEntity(connectingEntityId) as? ItemDisplay
        }

    private fun getDisplayTransform(from: Location, to: Location, entityLocation: Location): Matrix4f {
        return LineBuilder()
            .from(from.toVector() - entityLocation.toVector())
            .to(to.toVector() - entityLocation.toVector())
            .thickness(0.05)
            .build()
            .buildForItemDisplay()
    }

    @EventHandler(priority = EventPriority.LOW)
    private fun onElectricBlockBreak(event: RebarBlockBreakEvent) {
        val block = event.block as? ElectricRebarBlock ?: return
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
        val connectingNode = connectingEntity.persistentDataContainer.get(CONNECTING_NODE_KEY, RebarSerializers.UUID)
            ?.let(ElectricityManager::getNodeById) ?: return
        val connectingLocation = locations[connectingNode] ?: return
        val playerLocation = player.eyeLocation.subtract(0.0, 0.5, 0.0)
        if (connectingLocation.world != playerLocation.world) {
            deleteConnecting(player)
            return
        }
        connectingEntity.interpolationDelay = 0
        connectingEntity.interpolationDuration = 1
        connectingEntity.setTransformationMatrix(getDisplayTransform(connectingLocation, playerLocation, connectingEntity.location))
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
        if (item !is WireRebarItem) {
            deleteConnecting(player)
        } else {
            updateWireMaterial(player, item)
        }
    }

    @EventHandler
    private fun onPlayerInventorySlotChange(event: PlayerInventorySlotChangeEvent) {
        val player = event.player
        if (player.inventory.heldItemSlot != event.slot) return
        if (!player.persistentDataContainer.has(CONNECTING_KEY)) return
        val item = RebarItem.fromStack(event.newItemStack)
        if (item !is WireRebarItem) {
            deleteConnecting(player)
        } else {
            updateWireMaterial(player, item)
        }
    }
}