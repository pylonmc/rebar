package io.github.pylonmc.rebar.electricity

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.util.delayTicks
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.collections.ArrayDeque

object ElectricityManager {

    private val networks = mutableSetOf<ElectricNetwork>()

    private val nodes = mutableMapOf<UUID, ElectricNode>()

    private val transformers = mutableMapOf<Pair<ElectricNode.Connector, ElectricNode.Connector>, Double>()
    private val maxCurrents = mutableMapOf<Pair<ElectricNode, ElectricNode>, Double>()

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
        if (node is ElectricNode.Connector) {
            refreshNetworks(network)
        }
    }

    @JvmStatic
    fun getNodeById(id: UUID): ElectricNode? = nodes[id]

    @JvmStatic
    fun setTransformerEdge(from: ElectricNode.Connector, to: ElectricNode.Connector, final: Double) {
        transformers[from to to] = final
        from.onDisconnect { connector, other ->
            if (other == to) {
                transformers.remove(connector to other)
            }
        }
    }

    @JvmStatic
    fun getTransformerVoltage(from: ElectricNode.Connector, to: ElectricNode.Connector): Double? =
        transformers[from to to] ?: transformers[to to from]

    @JvmStatic
    fun setMaxCurrent(from: ElectricNode, to: ElectricNode, max: Double) {
        maxCurrents[from to to] = max
        from.onDisconnect { node, other ->
            if (other == to) {
                maxCurrents.remove(node to other)
            }
        }
    }

    @JvmStatic
    fun getMaxCurrent(from: ElectricNode, to: ElectricNode): Double =
        maxCurrents[from to to] ?: maxCurrents[to to from] ?: Double.MAX_VALUE

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