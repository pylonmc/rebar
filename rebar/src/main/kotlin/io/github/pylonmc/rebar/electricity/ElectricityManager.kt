package io.github.pylonmc.rebar.electricity

import java.util.UUID

object ElectricityManager {

    private val networks = mutableSetOf<ElectricNetwork>()

    private val nodes = mutableMapOf<UUID, ElectricNode>()

    fun addNode(node: ElectricNode) {
        nodes[node.id] = node
        networks.add(ElectricNetwork(node))
        mergeNetworks()
    }

    fun removeNode(node: ElectricNode) {
        nodes.remove(node.id)
        val network = networks.find { it.isPartOfNetwork(node) } ?: error("Node ${node.id} is not part of any network")
        if (node.type == ElectricNode.Type.CONNECTOR) {
            // A connector may separate a network into multiple parts, so we need to rebuild the networks of all other nodes in the same network
            networks.remove(network)
            for (otherNode in network.nodes) {
                if (otherNode.id != node.id) {
                    networks.add(ElectricNetwork(otherNode))
                }
            }
            mergeNetworks()
        } else {
            // A producer or consumer cannot separate a network, so we can just remove it from the network
            network.removeNode(node)
        }
    }

    private fun mergeNetworks() {
        val candidates = ArrayDeque(networks)
        networks.clear()
        while (candidates.isNotEmpty()) {
            var network = candidates.removeFirst()
            do {
                var merged = false
                for (i in candidates.indices) {
                    val candidate = candidates[i]
                    val mergedNetwork = ElectricNetwork.tryMerge(network, candidate)
                    if (mergedNetwork != null) {
                        network = mergedNetwork
                        candidates.removeAt(i)
                        merged = true
                        break
                    }
                }
            } while (merged)
            networks.add(network)
        }
    }
}