package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.electricity.ElectricNode
import io.github.pylonmc.rebar.electricity.ElectricityManager
import io.github.pylonmc.rebar.event.RebarBlockBreakEvent
import io.github.pylonmc.rebar.event.RebarBlockDeserializeEvent
import io.github.pylonmc.rebar.event.RebarBlockSerializeEvent
import io.github.pylonmc.rebar.event.RebarBlockUnloadEvent
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.jetbrains.annotations.ApiStatus
import java.util.*

interface RebarElectricBlock {

    @ApiStatus.NonExtendable
    fun <T : ElectricNode> addElectricNode(node: T): T {
        electricBlocks.getOrPut(this, ::mutableListOf).add(node)
        ElectricityManager.addNode(node)
        return node
    }

    @ApiStatus.NonExtendable
    fun getElectricNodes(): List<ElectricNode> = electricBlocks[this].orEmpty()

    @ApiStatus.Internal
    companion object : Listener {

        private val NODES_KEY = rebarKey("nodes")
        private val NODES_TYPE = RebarSerializers.LIST.listTypeFrom(ElectricNode.PDC_TYPE)

        private val electricBlocks = IdentityHashMap<RebarElectricBlock, MutableList<ElectricNode>>()

        @EventHandler
        private fun onDeserialize(event: RebarBlockDeserializeEvent) {
            val block = event.rebarBlock as? RebarElectricBlock ?: return
            val nodes = event.pdc.get(NODES_KEY, NODES_TYPE)!!.toMutableList()
            electricBlocks[block] = nodes

            for (node in nodes) {
                ElectricityManager.addNode(node)
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
            if (event.rebarBlock !is RebarElectricBlock) return
            for (node in electricBlocks.remove(event.rebarBlock).orEmpty()) {
                when (node) {
                    is ElectricNode.Connector -> {
                        for (connection in node.connections) {
                            ElectricityManager.getNodeById(connection)?.let { node.disconnect(it) }
                        }
                    }
                    is ElectricNode.Leaf<*> -> node.disconnect()
                }
                ElectricityManager.removeNode(node)
            }
        }
    }
}