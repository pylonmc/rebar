package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.electricity.ElectricityManager
import io.github.pylonmc.rebar.electricity.nodes.ElectricNode
import io.github.pylonmc.rebar.electricity.nodes.ElectricPortEntity
import io.github.pylonmc.rebar.electricity.nodes.ElectricPortSpec
import io.github.pylonmc.rebar.event.RebarBlockBreakEvent
import io.github.pylonmc.rebar.event.RebarBlockDeserializeEvent
import io.github.pylonmc.rebar.event.RebarBlockSerializeEvent
import io.github.pylonmc.rebar.event.RebarBlockUnloadEvent
import io.github.pylonmc.rebar.util.position.position
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.jetbrains.annotations.ApiStatus
import java.util.*

/**
 * A block that holds [ElectricNode]s and can have electric ports. Prefer using [SimpleElectricRebarBlock] unless that doesn't fit your needs.
 */

interface ElectricRebarBlock : EntityHolderRebarBlock {

    @ApiStatus.NonExtendable
    fun <T : ElectricNode> addElectricNode(node: T): T {
        electricBlocks.getOrPut(this, ::mutableMapOf)[node.name] = node
        ElectricityManager.addNode(node)
        return node
    }

    @get:ApiStatus.NonExtendable
    val electricNodes: List<ElectricNode>
        get() = electricBlocks[this].orEmpty().values.toList()

    @ApiStatus.NonExtendable
    fun getElectricNode(name: String) = electricBlocks.getOrPut(this, ::mutableMapOf)[name]

    @ApiStatus.NonExtendable
    fun getElectricNodeOrThrow(name: String) =
        getElectricNode(name) ?: throw NoSuchElementException("No electric node with name '$name' found in block at ${block.position}")

    @ApiStatus.NonExtendable
    fun <E : ElectricNode> getElectricNode(name: String, clazz: Class<E>): E? {
        val node = getElectricNode(name)
        if (!clazz.isInstance(node)) return null
        return clazz.cast(node)
    }

    @ApiStatus.NonExtendable
    fun <E : ElectricNode> getElectricNodeOrThrow(name: String, clazz: Class<E>): E =
        getElectricNode(name, clazz) ?: throw NoSuchElementException("No electric node '$name' of type ${clazz.simpleName} found")

    /**
     * Adds an electric node to this block that has a physical presence in the form of several display entities.
     */
    @ApiStatus.NonExtendable
    fun addElectricPort(port: ElectricPortSpec) {
        val node = port.node
        addEntity("port_${node.id}", ElectricPortEntity(block, port))
        addElectricNode(node)
    }

    @ApiStatus.Internal
    companion object : Listener {

        const val PORT_SCALE = 0.19f

        private val NODES_KEY = rebarKey("nodes")
        private val NODES_TYPE = RebarSerializers.LIST.listTypeFrom(ElectricNode.PDC_TYPE)

        private val electricBlocks = IdentityHashMap<ElectricRebarBlock, MutableMap<String, ElectricNode>>()

        @EventHandler(priority = EventPriority.MONITOR)
        private fun onDeserialize(event: RebarBlockDeserializeEvent) {
            val block = event.rebarBlock as? ElectricRebarBlock ?: return
            val nodes = event.pdc.get(NODES_KEY, NODES_TYPE)!!.toMutableList()
            electricBlocks[block] = nodes.associateByTo(mutableMapOf()) { it.name }

            for (node in nodes) {
                ElectricityManager.addNode(node)
            }
        }

        @EventHandler
        private fun onSerialize(event: RebarBlockSerializeEvent) {
            val block = event.rebarBlock as? ElectricRebarBlock ?: return
            event.pdc.set(NODES_KEY, NODES_TYPE, electricBlocks[block].orEmpty().values.toList())
        }

        @EventHandler
        private fun onUnload(event: RebarBlockUnloadEvent) {
            if (event.rebarBlock !is ElectricRebarBlock) return
            for (node in electricBlocks.remove(event.rebarBlock).orEmpty().values) {
                ElectricityManager.removeNode(node)
            }
        }

        @EventHandler
        private fun onBreak(event: RebarBlockBreakEvent) {
            val block = event.rebarBlock
            if (block !is ElectricRebarBlock) return
            for (node in electricBlocks.remove(block).orEmpty().values) {
                node.disconnectAll()
                ElectricityManager.removeNode(node)
            }
        }
    }
}

@JvmSynthetic
inline fun <reified E : ElectricNode> ElectricRebarBlock.getElectricNode(name: String): E? = getElectricNode(name, E::class.java)

@JvmSynthetic
inline fun <reified E : ElectricNode> ElectricRebarBlock.getElectricNodeOrThrow(name: String): E = getElectricNodeOrThrow(name, E::class.java)