package io.github.pylonmc.rebar.electricity

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.util.delayTicks
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.collections.ArrayDeque
import kotlin.collections.find
import kotlin.collections.indices
import kotlin.collections.isNotEmpty
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.collections.set
import kotlin.collections.toSet

object ElectricityManager {

    private val networks = mutableSetOf<ElectricNetwork>()

    private val nodes = mutableMapOf<UUID, ElectricNode>()

    init {
        Rebar.scope.launch {
            while (true) {
                for (network in networks) {
                    network.tick()
                }
                delayTicks(RebarConfig.ELECTRICITY_TICK_INTERVAL.toLong())
            }
        }
    }

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