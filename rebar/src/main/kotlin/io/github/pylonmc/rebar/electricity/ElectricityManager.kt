package io.github.pylonmc.rebar.electricity

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.electricity.nodes.ElectricNode
import io.github.pylonmc.rebar.event.RebarElectricNodeAddEvent
import io.github.pylonmc.rebar.event.RebarElectricNodeRemoveEvent
import io.github.pylonmc.rebar.util.delayTicks
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.collections.ArrayDeque

object ElectricityManager {

    private val networks = mutableSetOf<ElectricNetwork>()

    private val nodes = mutableMapOf<UUID, ElectricNode>()

    @JvmStatic
    @get:JvmName("dontAutoTickNetworks")
    @set:JvmName("dontAutoTickNetworks")
    var dontAutoTickNetworks = false

    init {
        Rebar.scope.launch {
            while (true) {
                if (!dontAutoTickNetworks) {
                    @Suppress("DEPRECATION")
                    tick()
                }
                delayTicks(RebarConfig.ELECTRICITY_TICK_INTERVAL.toLong())
            }
        }
    }

    @JvmStatic
    fun getNetworks() = networks.toSet()

    @JvmStatic
    fun addNode(node: ElectricNode) {
        nodes[node.id] = node
        networks.add(ElectricNetwork().also { it.addNode(node) })
        mergeNetworks(networks)
        RebarElectricNodeAddEvent(node).callEvent()
    }

    @JvmStatic
    fun removeNode(node: ElectricNode) {
        RebarElectricNodeRemoveEvent(node).callEvent()
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
        for (network in networks) {
            network.tick()
        }
    }

    @JvmSynthetic
    internal fun refreshNetwork(network: ElectricNetwork) {
        val candidates = mutableListOf<ElectricNetwork>()
        networks.remove(network)
        for (node in network.nodes) {
            candidates.add(ElectricNetwork().also { it.addNode(node) })
        }
        mergeNetworks(candidates)
    }

    /**
     * Attempts to merge any mergeable networks in [candidates]. All [candidates] are removed from the network list first,
     * then are attempted to be merged. Any merged results are added back into the network list, with the leftovers also
     * being re-added.
     */
    @JvmSynthetic
    internal fun mergeNetworks(candidates: Collection<ElectricNetwork>) {
        val candidates = ArrayDeque(candidates)
        for (candidate in candidates) {
            networks.remove(candidate)
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
            networks.add(network)
        }
    }

    @JvmSynthetic
    internal fun getNodeNetwork(node: ElectricNode): ElectricNetwork =
        networks.find { it.isPartOfNetwork(node) } ?: error("Node ${node.id} is not part of any network")
}