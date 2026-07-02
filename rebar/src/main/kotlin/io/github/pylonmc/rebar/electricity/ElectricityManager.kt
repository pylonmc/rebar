package io.github.pylonmc.rebar.electricity

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.electricity.nodes.ElectricNode
import io.github.pylonmc.rebar.util.delayTicks
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.collections.ArrayDeque

object ElectricityManager {

    private val _networks = mutableSetOf<ElectricNetwork>()

    @get:JvmStatic
    val networks: Set<ElectricNetwork> get() = _networks.toSet()

    private val nodes = mutableMapOf<UUID, ElectricNode>()

    init {
        Rebar.scope.launch {
            while (true) {
                if (!Rebar.debugMode) {
                    @Suppress("DEPRECATION")
                    tick()
                }
                delayTicks(RebarConfig.ELECTRICITY_TICK_INTERVAL.toLong())
            }
        }
    }

    @JvmStatic
    fun addNode(node: ElectricNode) {
        nodes[node.id] = node
        _networks.add(ElectricNetwork().also { it.addNode(node) })
        mergeNetworks(_networks)
    }

    @JvmStatic
    fun removeNode(node: ElectricNode) {
        nodes.remove(node.id)
        val network = node.network
        network.removeNode(node)
        refreshNetwork(network)
    }

    @JvmStatic
    fun getNodeById(id: UUID): ElectricNode? = nodes[id]

    /**
     * Only exposed for testing purposes
     */
    @JvmStatic
    @Deprecated("For testing purposes only")
    fun clear() {
        for (node in nodes.values.toList()) {
            removeNode(node)
        }
    }

    /**
     * Only exposed for testing purposes
     */
    @JvmStatic
    @Deprecated("For testing purposes only")
    fun tick() {
        for (network in _networks) {
            network.tick()
        }
    }

    @JvmSynthetic
    internal fun refreshNetwork(network: ElectricNetwork) {
        val candidates = mutableListOf<ElectricNetwork>()
        _networks.remove(network)
        for (node in network.nodes) {
            candidates.add(ElectricNetwork().also { it.addNode(node) })
        }
        mergeNetworks(candidates)
    }

    @JvmSynthetic
    internal fun mergeNetworks(candidates: Collection<ElectricNetwork>) {
        val candidates = ArrayDeque(candidates)
        for (candidate in candidates) {
            _networks.remove(candidate)
        }
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
            _networks.add(network)
        }
    }

    @JvmSynthetic
    internal fun getNodeNetwork(node: ElectricNode): ElectricNetwork =
        _networks.find { it.isPartOfNetwork(node) } ?: error("Node ${node.id} is not part of any network")
}