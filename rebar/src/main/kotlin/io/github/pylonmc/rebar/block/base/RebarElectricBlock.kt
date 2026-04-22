package io.github.pylonmc.rebar.block.base

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
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.Interaction
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.jetbrains.annotations.ApiStatus
import java.util.*

interface RebarElectricBlock : RebarEntityHolderBlock {

    @ApiStatus.NonExtendable
    fun <T : ElectricNode> addElectricNode(node: T): T {
        electricBlocks.getOrPut(this, ::mutableListOf).add(node)
        ElectricityManager.addNode(node)
        return node
    }

    @get:ApiStatus.NonExtendable
    val electricNodes: List<ElectricNode>
        get() = electricBlocks[this].orEmpty()

    /**
     * Adds an electric node to this block that has a physical presence in the form of several display entities.
     * [RebarDirectionalBlock.facing] must be set before calling this method.
     *
     * @return [node]
     */
    @ApiStatus.NonExtendable
    fun <T : ElectricNode> addElectricPort(face: BlockFace, radius: Double, overrideMaterial: Material?, node: T): T {
        val material = overrideMaterial ?: when (node) {
            is ElectricNode.Connector -> Material.GRAY_CONCRETE
            is ElectricNode.Consumer -> Material.GREEN_CONCRETE
            is ElectricNode.Producer -> Material.RED_CONCRETE
        }
        val expandedRadius = radius - PORT_OUTER_SCALE / 2 + 0.001
        addEntity(
            "outer_${node.id}", ItemDisplayBuilder()
                .itemStack(ItemStackBuilder.of(material).addCustomModelDataString("electric_port_outer"))
                .transformation(TransformBuilder().scale(PORT_OUTER_SCALE))
                .build(block.location.toCenterLocation().add(face.direction.multiply(expandedRadius)))
        )
        addEntity(
            "inner_${node.id}", ItemDisplayBuilder()
                .itemStack(ItemStackBuilder.of(Material.BLACK_CONCRETE).addCustomModelDataString("electric_port_inner"))
                .transformation(TransformBuilder().scale(PORT_INNER_SCALE))
                .build(block.location.toCenterLocation().add(face.direction.multiply(expandedRadius + 0.001 + PORT_OUTER_SCALE / 2 - PORT_INNER_SCALE / 2)))
        )
        val interaction = addEntity(
            "interaction_${node.id}", InteractionBuilder()
                .width(PORT_OUTER_SCALE)
                .height(PORT_OUTER_SCALE)
                .build(block.location.toCenterLocation().add(face.direction.multiply(radius - 0.001)))
        )
        interaction.persistentDataContainer.set(NODE_KEY, RebarSerializers.UUID, node.id)
        WireConnectionService.addInteraction(interaction, node)
        return addElectricNode(node)
    }

    /**
     * Adds an electric node to this block that has a physical presence in the form of several display entities.
     * [RebarDirectionalBlock.facing] must be set before calling this method. Radius defaults to 0.5.
     *
     * @return [node]
     */
    @ApiStatus.NonExtendable
    fun <T : ElectricNode> addElectricPort(face: BlockFace, node: T): T = addElectricPort(face, 0.5, null, node)

    @ApiStatus.Internal
    companion object : Listener {

        private const val PORT_OUTER_SCALE = 0.19f
        private const val PORT_INNER_SCALE = PORT_OUTER_SCALE / 2

        private val NODE_KEY = rebarKey("node")

        private val NODES_KEY = rebarKey("nodes")
        private val NODES_TYPE = RebarSerializers.LIST.listTypeFrom(ElectricNode.PDC_TYPE)

        private val electricBlocks = IdentityHashMap<RebarElectricBlock, MutableList<ElectricNode>>()

        @EventHandler(priority = EventPriority.MONITOR)
        private fun onDeserialize(event: RebarBlockDeserializeEvent) {
            val block = event.rebarBlock as? RebarElectricBlock ?: return
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
            val block = event.rebarBlock as? RebarElectricBlock ?: return
            event.pdc.set(NODES_KEY, NODES_TYPE, electricBlocks[block].orEmpty())
        }

        @EventHandler
        private fun onUnload(event: RebarBlockUnloadEvent) {
            if (event.rebarBlock !is RebarElectricBlock) return
            for (node in electricBlocks.remove(event.rebarBlock).orEmpty()) {
                ElectricityManager.removeNode(node)
            }
        }

        @EventHandler
        private fun onBreak(event: RebarBlockBreakEvent) {
            val block = event.rebarBlock
            if (block !is RebarElectricBlock) return
            for (node in electricBlocks.remove(block).orEmpty()) {
                when (node) {
                    is ElectricNode.Connector -> {
                        for (connection in node.connections) {
                            ElectricityManager.getNodeById(connection)?.let { node.disconnectFrom(it) }
                        }
                    }

                    is ElectricNode.Leaf -> node.disconnect()
                }
                ElectricityManager.removeNode(node)
            }
        }
    }
}