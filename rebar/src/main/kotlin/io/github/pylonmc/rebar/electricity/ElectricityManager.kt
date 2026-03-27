package io.github.pylonmc.rebar.electricity

import java.util.UUID

object ElectricityManager {

    private val networks = mutableSetOf<ElectricNetwork>()

    private val nodes = mutableMapOf<UUID, ElectricNode>()

    @JvmStatic
    fun addNode(node: ElectricNode) {
        nodes[node.id] = node
        networks.add(ElectricNetwork().also { it.addNode(node) })
        mergeNetworks()
    }

    @JvmStatic
    fun removeNode(node: ElectricNode) {
        nodes.remove(node.id)
        val network = node.network
        network.removeNode(node)
        if (node.type == ElectricNode.Type.CONNECTOR) {
            refreshNetworks(network)
        }
    }

    @JvmStatic
    fun getNodeById(id: UUID): ElectricNode? = nodes[id]

    @JvmSynthetic
    internal fun refreshNetworks(vararg networks: ElectricNetwork) {
        for (network in networks.toSet()) {
            this.networks.remove(network)
            for (node in network.nodes) {
                this.networks.add(ElectricNetwork().also { it.addNode(node) })
            }
        }
        mergeNetworks()
    }

    @JvmSynthetic
    internal fun mergeNetworks() {
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

    @JvmSynthetic
    internal fun getNodeNetwork(node: ElectricNode): ElectricNetwork =
        networks.find { it.isPartOfNetwork(node) } ?: error("Node ${node.id} is not part of any network")
}