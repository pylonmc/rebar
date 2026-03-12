package io.github.pylonmc.rebar.electricity

import java.util.UUID

class ElectricNetwork @JvmOverloads constructor(private val nodeMap: MutableMap<UUID, ElectricNode> = mutableMapOf()) {

    constructor(node: ElectricNode) : this(mutableMapOf(node.id to node))

    val nodes: Set<ElectricNode> get() = nodeMap.values.toSet()

    private val producers = nodes.filter { it.type == ElectricNode.Type.PRODUCER }.toMutableSet()
    private val consumers = nodes.filter { it.type == ElectricNode.Type.CONSUMER }.toMutableSet()

    fun isPartOfNetwork(node: ElectricNode): Boolean = node.id in nodeMap

    fun addNode(node: ElectricNode) {
        nodeMap[node.id] = node
        if (node.type == ElectricNode.Type.PRODUCER) {
            producers.add(node)
        } else if (node.type == ElectricNode.Type.CONSUMER) {
            consumers.add(node)
        }
    }

    fun removeNode(node: ElectricNode) {
        nodeMap.remove(node.id)
        producers.remove(node)
        consumers.remove(node)
    }

    companion object {
        fun tryMerge(network1: ElectricNetwork, network2: ElectricNetwork): ElectricNetwork? {
            if (network1.nodeMap.size > network2.nodeMap.size) {
                return tryMerge(network2, network1)
            }

            if (network1.nodeMap.values.any { network2.nodeMap.values.any(it::isConnectedTo) }) {
                val merged = ElectricNetwork()
                network1.nodeMap.values.forEach(merged::addNode)
                network2.nodeMap.values.forEach(merged::addNode)
                return merged
            } else {
                return null
            }
        }
    }
}