package io.github.pylonmc.rebar.electricity

import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.electricity.nodes.*
import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.world.storage
import it.unimi.dsi.fastutil.objects.Object2DoubleAVLTreeMap
import it.unimi.dsi.fastutil.objects.Object2DoubleMap
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2DoubleSortedMap
import org.bukkit.World
import java.util.PriorityQueue
import java.util.UUID
import kotlin.collections.ArrayDeque
import kotlin.math.abs
import kotlin.math.min

/**
 * A set of nodes where each node has at least one connection to another node in the set.
 */
class ElectricNetwork {

    private val nodeMap: MutableMap<UUID, ElectricNode> = mutableMapOf()
    val nodes: Set<ElectricNode> get() = nodeMap.values.toSet()

    private val producers = mutableSetOf<ElectricProducerNode>()
    private val consumers = mutableSetOf<ElectricConsumerNode>()
    private val acceptors = mutableSetOf<ElectricAcceptorNode>()

    /**
     * A map of heuristics based on distance to consumers.
     */
    private var heuristics: Map<ElectricNode, Map<ElectricNode, Int>>? = null

    private var snapshot: ConsumerSnapshot? = null

    fun addNode(node: ElectricNode) {
        nodeMap[node.id] = node
        when (node) {
            is ElectricProducerNode -> producers.add(node)
            is ElectricConsumerNode -> consumers.add(node)
            is ElectricAcceptorNode -> acceptors.add(node)
            is ElectricConnectorNode -> {}
        }
        heuristics = null
    }

    fun removeNode(node: ElectricNode) {
        nodeMap.remove(node.id)
        producers.remove(node)
        consumers.remove(node)
        acceptors.remove(node)
        heuristics = null
    }

    fun isPartOfNetwork(node: ElectricNode): Boolean = node.id in nodeMap

    fun markDirty() {
        snapshot = null
    }

    @JvmSynthetic
    internal fun producerChangedPower(producer: ElectricProducerNode, newPower: Double, increased: Boolean) {
        if (snapshot != null) {
            val snapshot = snapshot!!
            if (producer in snapshot.surplusProducers && !(increased && snapshot.hasUnpoweredConsumers)) {
                snapshot.surplusPower.put(producer, newPower)
            } else {
                this.snapshot = null
            }
        }
    }

    /**
     * When called, routes power from producers to consumers and acceptors (respecting the requirements of consumers
     * and the limits of edges), updates the power state of consumers, and calls handlers on producers and acceptors
     * with the amount of power produced/accepted.
     */
    fun tick() {
        if (snapshot == null) {
            distributePowerToConsumers()
        }

        val surplusPower = Object2DoubleAVLTreeMap(snapshot!!.surplusPower)
        val disconnectedEdges = snapshot!!.disconnectedEdges.toMutableSet()
        val edgeLoads = snapshot!!.edgeLoads

        distributePowerToAcceptors(surplusPower, disconnectedEdges, edgeLoads)

        for (producer in producers) {
            val taken = producer.power * POWER_ADJUSTMENT - (surplusPower[producer] ?: 0.0)
            producer.powerTakeHandler.accept(taken)
        }
    }

    private fun distributePowerToConsumers() {
        val validConsumers = Object2DoubleOpenHashMap<ElectricConsumerNode>()
        for (consumer in consumers) {
            val power = consumer.requiredPower
            if (power roughlyEquals 0.0) {
                consumer.isPowered = true
            } else {
                consumer.isPowered = false
                validConsumers.put(consumer, power)
            }
        }

        // First, we distribute power from producers to consumers
        val totalPowerProduced = producers.sumOf { it.power }
        var powerConsumedByConsumers = roundRobinFill(
            validConsumers,
            totalPowerProduced
        )

        var hasUnpoweredConsumers = false
        // If any consumer isn't getting enough power, we remove the one with the lowest requirement and try again,
        // until all remaining consumers are getting enough power, or we run out of consumers.
        while (powerConsumedByConsumers.any { (consumer, power) -> power < consumer.requiredPower }) {
            validConsumers.removeDouble(validConsumers.maxBy { it.value }.key)
            powerConsumedByConsumers = roundRobinFill(
                validConsumers,
                totalPowerProduced
            )
            hasUnpoweredConsumers = true
        }

        val sortedProducers = Object2DoubleAVLTreeMap(
            compareBy<ElectricProducerNode> { it.priority }
                .thenComparing { it.id }
        )
        for (producer in producers) {
            if (producer.power roughlyEquals 0.0) continue
            sortedProducers.put(producer, producer.power)
        }

        // Now that we know what consumes and produces what, we can try routing said power
        var edgeLoads: Object2DoubleMap<Edge> = Object2DoubleOpenHashMap()
        val disconnectedEdges = mutableSetOf<Edge>()
        for ((consumer, consumed) in powerConsumedByConsumers) {
            var powerLeft = consumed
            while (!(powerLeft roughlyEquals 0.0)) {
                var noPath = 0
                sortedProducers.object2DoubleEntrySet().removeIf { it.doubleValue roughlyEquals 0.0 }
                for (pair in sortedProducers.object2DoubleEntrySet()) {
                    val (producer, produced) = pair
                    val path = findBestPath(producer, consumer, disconnectedEdges)
                    if (path == null) {
                        noPath++
                        break
                    }

                    val loadResult = calculateLoadOnEdges(path, edgeLoads, produced)
                    val powerDelivered = min(loadResult.finalPower, powerLeft)
                    edgeLoads = loadResult.currents
                    powerLeft -= powerDelivered
                    pair.setValue(produced - powerDelivered)
                    for ((edge, load) in loadResult.currents) {
                        if (load roughlyEquals edge.powerLimit) {
                            disconnectedEdges.add(edge)
                        }
                    }
                    if (powerLeft roughlyEquals 0.0) break
                }

                if (noPath == sortedProducers.size) {
                    // no paths from any producer to this consumer, give up
                    break
                }
            }

            if (powerLeft roughlyEquals 0.0) {
                consumer.isPowered = true
            }
        }

        val surplusProducers = sortedProducers.filter { it.value roughlyEquals it.key.power }.keys

        snapshot = ConsumerSnapshot(sortedProducers, disconnectedEdges, edgeLoads, surplusProducers, hasUnpoweredConsumers)
    }

    private data class ConsumerSnapshot(
        val surplusPower: Object2DoubleSortedMap<ElectricProducerNode>,
        val disconnectedEdges: Set<Edge>,
        val edgeLoads: Object2DoubleMap<Edge>,
        val surplusProducers: Set<ElectricProducerNode>,
        val hasUnpoweredConsumers: Boolean,
    )

    private fun distributePowerToAcceptors(
        surplusPower: Object2DoubleSortedMap<ElectricProducerNode>,
        disconnectedEdges: MutableSet<Edge>,
        edgeLoads: Object2DoubleMap<Edge>
    ) {
        var edgeLoads = edgeLoads
        do {
            var notAccepted = 0
            acceptorLoop@ for (acceptor in acceptors) {
                var remaining = surplusPower.values.sum() / acceptors.size
                if (remaining roughlyEquals 0.0) {
                    notAccepted++
                    continue
                }
                var noPath = 0
                surplusPower.object2DoubleEntrySet().removeIf { it.doubleValue roughlyEquals 0.0 }
                for (pair in surplusPower) {
                    val (producer, surplus) = pair
                    val path = findBestPath(producer, acceptor, disconnectedEdges)
                    if (path == null) {
                        noPath++
                    } else {
                        val loadResult = calculateLoadOnEdges(path, edgeLoads, surplus)
                        val accepted = acceptor.handler.onAccept(loadResult.finalPower * POWER_ADJUSTMENT)
                        if (accepted roughlyEquals 0.0) {
                            notAccepted++
                            continue@acceptorLoop
                        } else {
                            edgeLoads = loadResult.currents
                            remaining -= accepted
                            pair.setValue(surplus - accepted)
                            for ((edge, load) in loadResult.currents) {
                                if (load roughlyEquals edge.powerLimit) {
                                    disconnectedEdges.add(edge)
                                }
                            }
                        }
                    }
                }

                if (noPath == surplusPower.size) {
                    // no paths from any producer to this acceptor, give up
                    notAccepted++
                }
            }
        } while (notAccepted != acceptors.size)
    }

    /**
     * Attempts to evenly distribute the given amount across the given keys, without exceeding the limits for any key.
     * If a key hits its limit, the excess is redistributed among the remaining keys, until either all excess is distributed
     * or all keys have hit their limits.
     */
    private fun <K> roundRobinFill(limits: Object2DoubleMap<K>, amount: Double): Object2DoubleMap<K> {
        val filled = Object2DoubleOpenHashMap<K>(limits.size)
        var remaining = amount
        val limits = Object2DoubleOpenHashMap(limits)
        while (!(remaining roughlyEquals 0.0) && limits.isNotEmpty()) {
            val fillAmount = remaining / limits.size
            for ((key, limit) in limits.object2DoubleEntrySet().toList()) {
                val toFill = min(limit, fillAmount)
                filled.mergeDouble(key, toFill, Double::plus)
                remaining -= toFill
                if (toFill >= limit) {
                    limits.removeDouble(key)
                } else {
                    limits[key] = limit - toFill
                }
            }
        }
        return filled
    }

    /**
     * Calculates the load for edges based on a greedy best first search from the producer to the consumer, using the graph distance to consumers as the heuristic.
     */
    // I would've used A*, but in this case, since the heuristic is perfect, greedy best first search is more efficient.
    private fun findBestPath(
        producer: ElectricNode,
        consumer: ElectricNode,
        disconnectedEdges: Set<Edge>,
    ): List<Edge>? {
        if (heuristics == null) heuristics = calculateDistanceHeuristics()
        val heuristic =
            heuristics!![consumer] ?: throw IllegalArgumentException("Target node is not a consumer in this network")
        val visited = mutableSetOf<ElectricNode>()
        val queue = PriorityQueue<ElectricNode>(compareBy { heuristic[it]!! })
        queue.add(producer)
        val inQueue = mutableSetOf(producer)
        val cameFrom = mutableMapOf<ElectricNode, ElectricNode>()

        while (queue.isNotEmpty()) {
            val current = queue.poll()
            inQueue.remove(current)
            if (current == consumer) {
                // reconstruct path
                val path = mutableListOf<Edge>()
                var node = consumer
                while (node != producer) {
                    val prev = cameFrom[node]!!
                    path.add(Edge(prev, node))
                    node = prev
                }
                return path.asReversed()
            }
            visited.add(current)
            for (neighborId in current.connections) {
                val neighbor = nodeMap[neighborId] ?: continue
                if (neighbor in visited) continue
                if (
                    Edge(current, neighbor) in disconnectedEdges ||
                    Edge(neighbor, current) in disconnectedEdges
                ) continue
                if (Edge(neighbor, current).unidirectional) continue
                if (neighbor !in inQueue) {
                    queue.add(neighbor)
                    inQueue.add(neighbor)
                    cameFrom[neighbor] = current
                }
            }
        }
        return null
    }

    private fun calculateLoadOnEdges(
        path: List<Edge>,
        existingLoads: Object2DoubleMap<Edge>,
        initialPower: Double
    ): LoadResult {
        if (initialPower roughlyEquals 0.0) {
            return LoadResult(existingLoads, 0.0)
        }
        val loads = Object2DoubleOpenHashMap(existingLoads)
        var currentPower = initialPower
        for (edge in path) {
            val remainingCapacity = edge.powerLimit - (loads[edge] ?: 0.0)
            currentPower = min(currentPower, remainingCapacity)
            loads.mergeDouble(edge, currentPower, Double::plus)
        }
        return if (currentPower roughlyEquals initialPower) {
            LoadResult(loads, currentPower)
        } else {
            // some limit has been hit somewhere, recalculate based on actual power delivered
            calculateLoadOnEdges(path, existingLoads, currentPower)
        }
    }

    private data class LoadResult(val currents: Object2DoubleMap<Edge>, val finalPower: Double)

    private fun calculateDistanceHeuristics(): Map<ElectricNode, Map<ElectricNode, Int>> {
        val heuristics = mutableMapOf<ElectricNode, Map<ElectricNode, Int>>()
        for (consumer in consumers + acceptors) {
            val queue = ArrayDeque(listOf(consumer to 0))
            val visited = mutableSetOf<ElectricNode>()
            val distanceMap = mutableMapOf<ElectricNode, Int>()

            while (queue.isNotEmpty()) {
                val (current, distance) = queue.removeFirst()
                if (current in visited) continue
                visited.add(current)
                distanceMap[current] = distance
                for (neighborId in current.connections) {
                    val neighbor = nodeMap[neighborId] ?: continue
                    if (neighbor in visited) continue
                    queue.add(neighbor to (distanceMap[current]!! + 1))
                }
            }
            heuristics[consumer] = distanceMap
        }
        return heuristics
    }

    data class Edge(val from: ElectricNode, val to: ElectricNode) {
        private val world: World
            get() = from.block.world!!

        private val powerLimitKey by lazy { rebarKey("edge_property_power_limit_${from.id}_${to.id}") }
        var powerLimit: Double
            get() = (world.storage[powerLimitKey, RebarSerializers.DOUBLE] ?: Double.MAX_VALUE)
            set(value) {
                world.storage[powerLimitKey, RebarSerializers.DOUBLE] = value
                from.network.markDirty()
            }

        private val unidirectionalKey by lazy { rebarKey("edge_property_unidirectional_${from.id}_${to.id}") }
        var unidirectional: Boolean
            get() = (world.storage[unidirectionalKey, RebarSerializers.BOOLEAN] ?: false)
            set(value) {
                world.storage[unidirectionalKey, RebarSerializers.BOOLEAN] = value
                from.network.markDirty()
            }
    }

    companion object {

        private val POWER_ADJUSTMENT = RebarConfig.ELECTRICITY_TICK_INTERVAL / 20.0

        fun tryMerge(network1: ElectricNetwork, network2: ElectricNetwork): ElectricNetwork? {
            if (network1.nodeMap.size > network2.nodeMap.size) {
                return tryMerge(network2, network1)
            }

            if (network1.nodeMap.values.any { network2.nodeMap.values.any(it::isConnectedTo) }) {
                val merged = ElectricNetwork()
                merged.nodeMap.putAll(network1.nodeMap)
                merged.nodeMap.putAll(network2.nodeMap)
                merged.producers.addAll(network1.producers)
                merged.producers.addAll(network2.producers)
                merged.consumers.addAll(network1.consumers)
                merged.consumers.addAll(network2.consumers)
                merged.acceptors.addAll(network1.acceptors)
                merged.acceptors.addAll(network2.acceptors)
                return merged
            } else {
                return null
            }
        }
    }
}

private infix fun Double.roughlyEquals(other: Double): Boolean = abs(this - other) < 1e-6