package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.electricity.ElectricNode
import io.github.pylonmc.rebar.electricity.ElectricityManager
import io.github.pylonmc.rebar.event.RebarBlockBreakEvent
import io.github.pylonmc.rebar.event.RebarBlockDeserializeEvent
import io.github.pylonmc.rebar.event.RebarBlockSerializeEvent
import io.github.pylonmc.rebar.event.RebarBlockUnloadEvent
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.jetbrains.annotations.ApiStatus
import java.util.IdentityHashMap

interface RebarElectricBlock {

    @ApiStatus.NonExtendable
    fun createElectricNode(node: ElectricNode) {
        electricBlocks.getOrPut(this, ::mutableListOf).add(node)
        ElectricityManager.addNode(node)
    }

    @ApiStatus.NonExtendable
    fun createElectricNode(connectionPoint: Location, type: ElectricNode.Type) {
        createElectricNode(ElectricNode(connectionPoint, type))
    }
    
    @ApiStatus.NonExtendable
    fun getElectricNodes(): List<ElectricNode> = electricBlocks[this].orEmpty()

    companion object : Listener {

        private val nodesKey = rebarKey("nodes")
        private val nodesType = RebarSerializers.LIST.listTypeFrom(ElectricNode.PDC_TYPE)

        private val electricBlocks = IdentityHashMap<RebarElectricBlock, MutableList<ElectricNode>>()

        @EventHandler
        private fun onDeserialize(event: RebarBlockDeserializeEvent) {
            val block = event.rebarBlock
            if (block !is RebarElectricBlock) return
            val nodes = event.pdc.get(nodesKey, nodesType)!!.toMutableList()
            electricBlocks[block] = nodes

            for (node in nodes) {
                ElectricityManager.addNode(node)
            }
        }

        @EventHandler
        private fun onSerialize(event: RebarBlockSerializeEvent) {
            val block = event.rebarBlock
            if (block !is RebarElectricBlock) return
            event.pdc.set(nodesKey, nodesType, electricBlocks[block].orEmpty())
        }

        @EventHandler
        private fun onUnload(event: RebarBlockUnloadEvent) {
            if (event.rebarBlock is RebarElectricBlock) {
                for (node in electricBlocks.remove(event.rebarBlock)!!) {
                    ElectricityManager.removeNode(node)
                }
            }
        }

        @EventHandler
        private fun onBreak(event: RebarBlockBreakEvent) {
            if (event.rebarBlock is RebarElectricBlock) {
                for (node in electricBlocks.remove(event.rebarBlock)!!) {
                    for (connection in node.connections) {
                        ElectricityManager.getNodeById(connection)?.disconnect(node)
                    }
                    ElectricityManager.removeNode(node)
                }
            }
        }
    }
}