package io.github.pylonmc.rebar.electricity

import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.electricity.ElectricNode.Edge
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

    private val producers = mutableSetOf<ElectricNode.Producer>()
    private val consumers = mutableSetOf<ElectricNode.Consumer>()
    private val acceptors = mutableSetOf<ElectricNode.Acceptor>()

    /**
     * A map of heuristics based on distance to consumers.
     */
    private var heuristics: Map<ElectricNode, Map<ElectricNode, Int>>? = null

    fun addNode(node: ElectricNode) {
        nodeMap[node.id] = node
        when (node) {
            is ElectricNode.Producer -> producers.add(node)
            is ElectricNode.Consumer -> consumers.add(node)
            is ElectricNode.Acceptor -> acceptors.add(node)
            is ElectricNode.Connector -> {}
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

    /**
     * When called, routes power from producers to consumers and acceptors (respecting the requirements of consumers
     * and the limits of edges), updates the power state of consumers, and calls handlers on producers and acceptors
     * with the amount of power produced/accepted.
     */
    // TODO memoization if performance is bad
    fun tick() {
        for (consumer in consumers) {
            consumer.isPowered = consumer.requiredPower roughlyEquals 0.0
        }

        val surplusPower = producers.associateWithTo(mutableMapOf()) { it.power }

        // First, we distribute power from producers to consumers
        val totalPowerProduced = producers.sumOf { it.power }
        val validConsumers = consumers.associateWith { it.requiredPower }.filterTo(mutableMapOf()) { it.value > 0 }
        var powerConsumedByConsumers = roundRobinFill(
            validConsumers,
            totalPowerProduced
        )

        // If any consumer isn't getting enough power, we remove the one with the lowest requirement and try again,
        // until all remaining consumers are getting enough power, or we run out of consumers.
        while (powerConsumedByConsumers.any { (consumer, power) -> power < consumer.requiredPower }) {
            validConsumers.remove(validConsumers.minBy { it.value }.key)
            powerConsumedByConsumers = roundRobinFill(
                validConsumers,
                totalPowerProduced
            )
        }

        // Then we invert that: knowing how much power was consumed, we calculate how much was taken from each producer
        val powerTakenFromProducers = roundRobinFill(
            producers.associateWith { it.power }.filterValues { it > 0 },
            powerConsumedByConsumers.values.sum()
        ).toMutableMap()

        for ((producer, taken) in powerTakenFromProducers) {
            surplusPower[producer] = surplusPower[producer]!! - taken
        }

        // Now that we know what consumes and produces what, we can try routing said power
        val limits = mutableMapOf<Edge, Double>()
        var edgeLoads = mapOf<Edge, Double>()
        val disconnectedEdges = mutableSetOf<Edge>()
        for ((consumer, consumed) in powerConsumedByConsumers) {
            var powerLeft = consumed
            while (!(powerLeft roughlyEquals 0.0)) {
                var noPath = 0
                for ((producer, produced) in powerTakenFromProducers) {
                    if (produced roughlyEquals 0.0) continue
                    val path = findBestPath(producer, consumer, disconnectedEdges)
                    if (path == null) {
                        noPath++
                        break
                    }

                    // Determine limits on edges if not already known
                    for (edge in path) {
                        if (edge in limits) continue
                        limits[edge] = EdgeProperty.getProperty<EdgeProperty.PowerLimit>(edge)?.value ?: Double.POSITIVE_INFINITY
                    }

                    val loadResult = calculateLoadOnEdges(path, edgeLoads, limits, produced)
                    val powerDelivered = min(loadResult.finalPower, powerLeft)
                    edgeLoads = loadResult.currents
                    powerLeft -= powerDelivered
                    powerTakenFromProducers[producer] = powerTakenFromProducers[producer]!! - powerDelivered
                    for ((edge, load) in loadResult.currents) {
                        if (load roughlyEquals limits[edge]!!) {
                            disconnectedEdges.add(edge)
                        }
                    }
                }

                for ((producer, produced) in powerTakenFromProducers.toList()) {
                    if (produced roughlyEquals 0.0) {
                        powerTakenFromProducers.remove(producer)
                    }
                }

                if (noPath == powerTakenFromProducers.size) {
                    // no paths from any producer to this consumer, give up
                    break
                }
            }

            if (powerLeft roughlyEquals 0.0 && consumed roughlyEquals consumer.requiredPower) {
                consumer.isPowered = true
            }
        }

        for ((producer, taken) in powerTakenFromProducers) {
            surplusPower[producer] = surplusPower[producer]!! + taken
        }

        do {
            var notAccepted = 0
            acceptorLoop@ for (acceptor in acceptors) {
                var remaining = surplusPower.values.sum() / acceptors.size
                if (remaining roughlyEquals 0.0) {
                    notAccepted++
                    continue
                }
                var noPath = 0
                for ((producer, surplus) in surplusPower) {
                    if (surplus roughlyEquals 0.0) continue
                    val path = findBestPath(producer, acceptor, disconnectedEdges)
                    if (path == null) {
                        noPath++
                    } else {
                        // Determine limits on edges if not already known
                        for (edge in path) {
                            if (edge in limits) continue
                            limits[edge] = EdgeProperty.getProperty<EdgeProperty.PowerLimit>(edge)?.value ?: Double.POSITIVE_INFINITY
                        }

                        val loadResult = calculateLoadOnEdges(path, edgeLoads, limits, surplus)
                        val accepted = acceptor.handler.onAccept(loadResult.finalPower * POWER_ADJUSTMENT)
                        if (accepted roughlyEquals 0.0) {
                            notAccepted++
                            continue@acceptorLoop
                        } else {
                            edgeLoads = loadResult.currents
                            remaining -= accepted
                            surplusPower[producer] = surplusPower[producer]!! - accepted
                            for ((edge, load) in loadResult.currents) {
                                if (load roughlyEquals limits[edge]!!) {
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

                for ((producer, surplus) in surplusPower.toList()) {
                    if (surplus roughlyEquals 0.0) {
                        surplusPower.remove(producer)
                    }
                }
            }
        } while (notAccepted != acceptors.size)

        for (producer in producers) {
            val taken = producer.power * POWER_ADJUSTMENT - (surplusPower[producer] ?: 0.0)
            producer.powerTakeHandler.accept(taken)
        }
    }

    /**
     * Attempts to evenly distribute the given amount across the given keys, without exceeding the limits for any key.
     * If a key hits its limit, the excess is redistributed among the remaining keys, until either all excess is distributed
     * or all keys have hit their limits.
     */
    private fun <K> roundRobinFill(limits: Map<K, Double>, amount: Double): Map<K, Double> {
        val filled = mutableMapOf<K, Double>()
        var remaining = amount
        val limits = limits.toMutableMap()
        while (!(remaining roughlyEquals 0.0) && limits.isNotEmpty()) {
            val fillAmount = remaining / limits.size
            for ((key, limit) in limits.toList()) {
                val toFill = min(limit, fillAmount)
                filled.merge(key, toFill, Double::plus)
                remaining -= toFill
                if (toFill >= limit) {
                    limits.remove(key)
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
                if (EdgeProperty.Unidirectional in EdgeProperty.getProperties(Edge(neighbor, current))) continue
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
        existingLoads: Map<Edge, Double>,
        limits: Map<Edge, Double>,
        initialPower: Double
    ): LoadResult {
        if (initialPower roughlyEquals 0.0) {
            return LoadResult(existingLoads, 0.0)
        }
        val loads = existingLoads.toMutableMap()
        var currentPower = initialPower
        for (edge in path) {
            val remainingCapacity = limits[edge]!! - (loads[edge] ?: 0.0)
            currentPower = min(currentPower, remainingCapacity)
            loads.merge(edge, currentPower, Double::plus)
        }
        return if (currentPower roughlyEquals initialPower) {
            LoadResult(loads, currentPower)
        } else {
            // some limit has been hit somewhere, recalculate based on actual power delivered
            calculateLoadOnEdges(path, existingLoads, limits, currentPower)
        }
    }

    private data class LoadResult(val currents: Map<Edge, Double>, val finalPower: Double)

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