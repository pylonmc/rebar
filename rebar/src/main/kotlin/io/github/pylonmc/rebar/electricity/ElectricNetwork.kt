package io.github.pylonmc.rebar.electricity

import java.util.UUID

class ElectricNetwork(private val nodeMap: MutableMap<UUID, ElectricNode> = mutableMapOf()) {

    constructor(node: ElectricNode) : this(mutableMapOf(node.id to node))

    val nodes: Set<ElectricNode> get() = nodeMap.values.toSet()

    fun isPartOfNetwork(node: ElectricNode): Boolean = node.id in nodeMap

    fun addNode(node: ElectricNode) {
        nodeMap[node.id] = node
    }

    fun removeNode(node: ElectricNode) {
        nodeMap.remove(node.id)
    }

    companion object {
        fun tryMerge(network1: ElectricNetwork, network2: ElectricNetwork): ElectricNetwork? {
            if (network1.nodeMap.size > network2.nodeMap.size) {
                return tryMerge(network2, network1)
            }

            if (network1.nodeMap.values.any { node1 -> network2.nodeMap.keys.any { node2 -> node2 in node1.connections } }) {
                val merged = ElectricNetwork()
                merged.nodeMap.putAll(network1.nodeMap)
                merged.nodeMap.putAll(network2.nodeMap)
                return merged
            } else {
                return null
            }
        }
    }
}