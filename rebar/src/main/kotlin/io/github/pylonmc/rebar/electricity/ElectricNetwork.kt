package io.github.pylonmc.rebar.electricity

import java.util.PriorityQueue
import java.util.UUID
import kotlin.collections.ArrayDeque
import kotlin.math.abs
import kotlin.math.min

class ElectricNetwork {

    private val nodeMap: MutableMap<UUID, ElectricNode> = mutableMapOf()
    val nodes: Set<ElectricNode> get() = nodeMap.values.toSet()

    private val producers = mutableSetOf<ElectricNode.Producer>()
    private val consumers = mutableSetOf<ElectricNode.Consumer>()
    private val acceptors = mutableSetOf<ElectricNode.Acceptor>()

    /**
     * A map of heuristics based on distance to consumers.
     */
    private val heuristics = mutableMapOf<ElectricNode, Map<ElectricNode, Int>>()

    fun addNode(node: ElectricNode) {
        nodeMap[node.id] = node
        when (node) {
            is ElectricNode.Producer -> producers.add(node)
            is ElectricNode.Consumer -> consumers.add(node)
            is ElectricNode.Acceptor -> acceptors.add(node)
            is ElectricNode.Connector -> {}
        }
        heuristics.clear()
    }

    fun removeNode(node: ElectricNode) {
        nodeMap.remove(node.id)
        producers.remove(node)
        consumers.remove(node)
        acceptors.remove(node)
        heuristics.clear()
    }

    fun isPartOfNetwork(node: ElectricNode): Boolean = node.id in nodeMap

    fun tick() {
        for (consumer in consumers) {
            consumer.isPowered = false
        }

        val surplusPower = producers.associateWith { it.power }.toMutableMap()

        // First, we distribute power from producers to consumers
        val powerConsumedByConsumers = roundRobinFill(
            consumers.associateWith { it.requiredPower },
            producers.sumOf { it.power }
        )

        // Then we invert that: knowing how much power was consumed, we calculate how much was taken from each producer
        val powerTakenFromProducers = roundRobinFill(
            producers.associateWith { it.power },
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
                    val tempDisconnectedEdges = mutableSetOf<Edge>()
                    while (true) {
                        val path = findBestPath(producer, consumer, disconnectedEdges + tempDisconnectedEdges)
                        if (path == null) {
                            noPath++
                            break
                        }

                        // Determine limits on edges if not already known
                        for (edge in path) {
                            if (edge in limits) continue
                            limits[edge] = ElectricityManager.getMaxCurrent(edge.from, edge.to)
                        }

                        val loadResult =
                            calculateLoadOnEdges(path, edgeLoads, limits, produced, producer.voltage)
                        val powerDelivered = min(loadResult.finalPower, powerLeft)
                        if (loadResult.finalVoltage !in consumer.voltageRange) {
                            // voltage out of range, disconnect last edge and try again
                            tempDisconnectedEdges.add(path.last())
                        } else {
                            edgeLoads = loadResult.currents
                            powerLeft -= powerDelivered
                            powerTakenFromProducers[producer] = powerTakenFromProducers[producer]!! - powerDelivered
                            for ((edge, load) in loadResult.currents) {
                                if (load roughlyEquals limits[edge]!!) {
                                    disconnectedEdges.add(edge)
                                }
                            }
                            break
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
                            limits[edge] = ElectricityManager.getMaxCurrent(edge.from, edge.to)
                        }

                        val loadResult =
                            calculateLoadOnEdges(path, edgeLoads, limits, surplus, producer.voltage)
                        val accepted = acceptor.handler.onAccept(loadResult.finalPower)
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
            val taken = producer.power - (surplusPower[producer] ?: 0.0)
            producer.powerTakeHandler.accept(taken)
        }
    }

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
        if (heuristics.isEmpty()) recalculateDistanceHeuristics()
        val heuristic = heuristics[consumer]
            ?: throw IllegalArgumentException("Target node is not a consumer in this network")
        val visited = mutableSetOf<ElectricNode>()
        val queue = PriorityQueue<ElectricNode>(compareBy { heuristic[it]!! })
        queue.add(producer)
        val inQueue = mutableSetOf(producer)
        val cameFrom = mutableMapOf<ElectricNode, ElectricNode>()

        fun queueNeighbor(node: ElectricNode, neighborId: UUID) {
            val neighbor = nodeMap[neighborId] ?: return
            if (neighbor in visited) return
            if (Edge(node, neighbor) in disconnectedEdges || Edge(neighbor, node) in disconnectedEdges) return
            if (neighbor !in inQueue) {
                queue.add(neighbor)
                inQueue.add(neighbor)
                cameFrom[neighbor] = node
            }
        }

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
            when (current) {
                is ElectricNode.Connector -> {
                    for (neighborId in current.connections) {
                        queueNeighbor(current, neighborId)
                    }
                }

                is ElectricNode.Leaf -> current.connection?.let { queueNeighbor(current, it) }
            }
        }
        return null
    }

    private fun calculateLoadOnEdges(
        path: List<Edge>,
        existingLoads: Map<Edge, Double>,
        limits: Map<Edge, Double>,
        initialPower: Double,
        initialVoltage: Double
    ): LoadResult {
        if (initialPower roughlyEquals 0.0 || initialVoltage roughlyEquals 0.0) {
            return LoadResult(existingLoads, 0.0, initialVoltage)
        }
        val loads = existingLoads.toMutableMap()
        var currentPower = initialPower
        var currentVoltage = initialVoltage
        for (edge in path) {
            if (edge.from is ElectricNode.Connector && edge.to is ElectricNode.Connector) {
                val transformerVoltage = ElectricityManager.getTransformerVoltage(edge.from, edge.to)
                if (transformerVoltage != null) {
                    currentVoltage = transformerVoltage
                }
            }
            val remainingCapacity = limits[edge]!! - (loads[edge] ?: 0.0)
            val current = min(currentPower / currentVoltage, remainingCapacity)
            loads.merge(edge, current, Double::plus)
            currentPower = current * currentVoltage
        }
        return if (currentPower roughlyEquals initialPower) {
            LoadResult(loads, currentPower, currentVoltage)
        } else {
            // some limit has been hit somewhere, recalculate based on actual power delivered
            calculateLoadOnEdges(path, existingLoads, limits, currentPower, initialVoltage)
        }
    }

    private data class LoadResult(val currents: Map<Edge, Double>, val finalPower: Double, val finalVoltage: Double)

    private fun recalculateDistanceHeuristics() {
        heuristics.clear()
        for (consumer in consumers + acceptors) {
            val queue = ArrayDeque<Pair<ElectricNode, Int>>(listOf(consumer to 0))
            val visited = mutableSetOf<ElectricNode>()
            val distanceMap = mutableMapOf<ElectricNode, Int>()

            fun queueNeighbor(node: ElectricNode, neighborId: UUID) {
                val neighbor = nodeMap[neighborId] ?: return
                if (neighbor in visited) return
                queue.add(neighbor to (distanceMap[node]!! + 1))
            }

            while (queue.isNotEmpty()) {
                val (current, distance) = queue.removeFirst()
                if (current in visited) continue
                visited.add(current)
                distanceMap[current] = distance
                when (current) {
                    is ElectricNode.Connector -> {
                        for (neighborId in current.connections) {
                            queueNeighbor(current, neighborId)
                        }
                    }

                    is ElectricNode.Leaf -> current.connection?.let { queueNeighbor(current, it) }
                }
            }
            heuristics[consumer] = distanceMap
        }
    }

    private data class Edge(val from: ElectricNode, val to: ElectricNode)

    companion object {
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