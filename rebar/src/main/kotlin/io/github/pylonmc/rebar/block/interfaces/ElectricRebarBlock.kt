package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.electricity.ElectricNode
import io.github.pylonmc.rebar.electricity.ElectricityManager
import io.github.pylonmc.rebar.electricity.WireConnectionService
import io.github.pylonmc.rebar.entity.display.InteractionBuilder
import io.github.pylonmc.rebar.entity.display.ItemDisplayBuilder
import io.github.pylonmc.rebar.entity.display.transform.TransformBuilder
import io.github.pylonmc.rebar.event.RebarBlockBreakEvent
import io.github.pylonmc.rebar.event.RebarBlockDeserializeEvent
import io.github.pylonmc.rebar.event.RebarBlockSerializeEvent
import io.github.pylonmc.rebar.event.RebarBlockUnloadEvent
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.util.Vectors
import io.github.pylonmc.rebar.util.plus
import io.github.pylonmc.rebar.util.position.position
import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.util.times
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.Interaction
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.util.Vector
import org.jetbrains.annotations.ApiStatus
import java.util.*

interface ElectricRebarBlock : EntityHolderRebarBlock {

    @ApiStatus.NonExtendable
    fun <T : ElectricNode> addElectricNode(node: T): T {
        electricBlocks.getOrPut(this, ::mutableListOf).add(node)
        ElectricityManager.addNode(node)
        return node
    }

    @get:ApiStatus.NonExtendable
    val electricNodes: List<ElectricNode>
        get() = electricBlocks[this].orEmpty()

    @ApiStatus.NonExtendable
    fun getElectricNode(name: String) = electricNodes.find { it.name == name }

    @ApiStatus.NonExtendable
    fun getElectricNodeOrThrow(name: String) = getElectricNode(name) ?: throw NoSuchElementException("No electric node with name '$name' found in block at ${block.position}")

    /**
     * Adds an electric node to this block that has a physical presence in the form of several display entities.
     */
    @ApiStatus.NonExtendable
    fun addElectricPort(port: ElectricPort) {
        val (node, face, radius, offset, material) = port
        val expandedRadius = radius - PORT_OUTER_SCALE / 2 + 0.001
        addEntity(
            "outer_${node.id}", ItemDisplayBuilder()
                .itemStack(ItemStackBuilder.of(material).addCustomModelDataString("electric_port_outer"))
                .transformation(TransformBuilder().scale(PORT_OUTER_SCALE))
                .build(block.location.toCenterLocation().add(face.direction * expandedRadius + offset))
        )
        addEntity(
            "inner_${node.id}", ItemDisplayBuilder()
                .itemStack(ItemStackBuilder.of(Material.BLACK_CONCRETE).addCustomModelDataString("electric_port_inner"))
                .transformation(TransformBuilder().scale(PORT_INNER_SCALE))
                .build(block.location.toCenterLocation().add(face.direction * (expandedRadius + 0.001 + PORT_OUTER_SCALE / 2 - PORT_INNER_SCALE / 2) + offset))
        )
        val interaction = addEntity(
            "interaction_${node.id}", InteractionBuilder()
                .width(PORT_OUTER_SCALE)
                .height(PORT_OUTER_SCALE)
                .build(block.location.toCenterLocation().add(face.direction * (radius - 0.001) + offset))
        )
        interaction.persistentDataContainer.set(NODE_KEY, RebarSerializers.UUID, node.id)
        WireConnectionService.addInteraction(interaction, node)
        addElectricNode(node)
    }

    @JvmRecord
    data class ElectricPort @JvmOverloads constructor(
        val node: ElectricNode,
        val face: BlockFace,
        val radius: Double = 0.5,
        val offset: Vector = Vectors.zero,
        val material: Material = when (node) {
            is ElectricNode.Connector -> Material.GRAY_CONCRETE
            is ElectricNode.Consumer, is ElectricNode.Acceptor -> Material.LIME_CONCRETE
            is ElectricNode.Producer -> Material.RED_CONCRETE
        }
    ) {
        fun radius(radius: Double) = copy(radius = radius)
        fun offset(offset: Vector) = copy(offset = offset)
        fun material(material: Material) = copy(material = material)
    }

    @ApiStatus.Internal
    companion object : Listener {

        private const val PORT_OUTER_SCALE = 0.19f
        private const val PORT_INNER_SCALE = PORT_OUTER_SCALE / 2

        private val NODE_KEY = rebarKey("node")

        private val NODES_KEY = rebarKey("nodes")
        private val NODES_TYPE = RebarSerializers.LIST.listTypeFrom(ElectricNode.PDC_TYPE)

        private val electricBlocks = IdentityHashMap<ElectricRebarBlock, MutableList<ElectricNode>>()

        @EventHandler(priority = EventPriority.MONITOR)
        private fun onDeserialize(event: RebarBlockDeserializeEvent) {
            val block = event.rebarBlock as? ElectricRebarBlock ?: return
            val nodes = event.pdc.get(NODES_KEY, NODES_TYPE)!!.toMutableList()
            electricBlocks[block] = nodes

            for (node in nodes) {
                ElectricityManager.addNode(node)
            }

            for ((_, id) in block.heldEntities) {
                val interaction = Bukkit.getEntity(id) as? Interaction ?: continue
                val nodeId = interaction.persistentDataContainer.get(NODE_KEY, RebarSerializers.UUID) ?: continue
                val node = ElectricityManager.getNodeById(nodeId) ?: continue
                WireConnectionService.addInteraction(interaction, node)
            }
        }

        @EventHandler
        private fun onSerialize(event: RebarBlockSerializeEvent) {
            val block = event.rebarBlock as? ElectricRebarBlock ?: return
            event.pdc.set(NODES_KEY, NODES_TYPE, electricBlocks[block].orEmpty())
        }

        @EventHandler
        private fun onUnload(event: RebarBlockUnloadEvent) {
            if (event.rebarBlock !is ElectricRebarBlock) return
            for (node in electricBlocks.remove(event.rebarBlock).orEmpty()) {
                ElectricityManager.removeNode(node)
            }
        }

        @EventHandler
        private fun onBreak(event: RebarBlockBreakEvent) {
            val block = event.rebarBlock
            if (block !is ElectricRebarBlock) return
            for (node in electricBlocks.remove(block).orEmpty()) {
                node.disconnectAll()
                ElectricityManager.removeNode(node)
            }
        }
    }
}