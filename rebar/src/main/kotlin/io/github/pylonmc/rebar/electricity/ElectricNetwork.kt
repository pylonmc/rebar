package io.github.pylonmc.rebar.electricity

import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.block.base.RebarElectricBlock
import java.util.PriorityQueue
import java.util.UUID
import kotlin.collections.ArrayDeque
import kotlin.math.abs
import kotlin.math.min

class ElectricNetwork {

    private val nodeMap: MutableMap<UUID, ElectricNode> = mutableMapOf()
    val nodes: Set<ElectricNode> get() = nodeMap.values.toSet()

    private val producers = mutableSetOf<ElectricNode>()
    private val consumers = mutableSetOf<ElectricNode>()

    /**
     * A map of heuristics based on distance to consumers.
     */
    private val heuristics = mutableMapOf<ElectricNode, Map<ElectricNode, Int>>()

    fun addNode(node: ElectricNode) {
        nodeMap[node.id] = node
        if (node.type == ElectricNode.Type.PRODUCER) {
            producers.add(node)
        } else if (node.type == ElectricNode.Type.CONSUMER) {
            consumers.add(node)
        }
        heuristics.clear()
    }

    fun removeNode(node: ElectricNode) {
        nodeMap.remove(node.id)
        producers.remove(node)
        consumers.remove(node)
        blocks.remove(node)
        heuristics.clear()
    }

    fun isPartOfNetwork(node: ElectricNode): Boolean = node.id in nodeMap

    private val blocks = mutableMapOf<ElectricNode, RebarElectricBlock>()

    private inline fun <reified T : RebarElectricBlock> ElectricNode.getBlock(): T? =
        blocks.getOrPut(this) { BlockStorage.getAsOrThrow(block) } as? T

    private val ElectricNode.producerBlock: RebarElectricBlock.Producer
        get() = getBlock() ?: error("Expected producer block for node $this")
    private val ElectricNode.consumerBlock: RebarElectricBlock.Consumer
        get() = getBlock() ?: error("Expected consumer block for node $this")
    private val ElectricNode.maybeConnectorBlock: RebarElectricBlock.Connector? get() = getBlock()

    fun tick() {
        for (consumer in consumers) {
            RebarElectricBlock.Consumer.setPowered(consumer.consumerBlock, false)
        }

        // First, we distribute power from producers to consumer
        val powerConsumedByConsumers = roundRobinFill(
            consumers.associateWith { it.consumerBlock.requiredPower },
            producers.sumOf { it.producerBlock.power }
        )

        // Then we invert that, knowing how much power was consumed, we calculate how much was taken from each producer
        val powerTakenFromProducers = roundRobinFill(
            producers.associateWith { it.producerBlock.power },
            powerConsumedByConsumers.values.sum()
        ).toMutableMap()

        // Now that we know what consumes and produces what, we can try routing said power
        val limits = mutableMapOf<Edge, Double>()
        var edgeLoads = mapOf<Edge, Double>()
        val disconnectedEdges = mutableSetOf<Edge>()
        for ((consumer, consumed) in powerConsumedByConsumers) {
            var powerLeft = consumed
            val consumerBlock = consumer.consumerBlock
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

                        // Determine limits on edges if not alreayd known
                        for (edge in path) {
                            if (edge in limits) continue
                            val connectorBlock = edge.from.maybeConnectorBlock
                            if (connectorBlock != null) {
                                limits[edge] = connectorBlock.getCurrentLimit(edge.to)
                            } else {
                                val connectorBlock = edge.to.maybeConnectorBlock
                                if (connectorBlock != null) {
                                    limits[edge] = connectorBlock.getCurrentLimit(edge.from)
                                }
                            }
                        }

                        val loadResult = calculateLoadOnEdges(path, edgeLoads, limits, produced, producer.producerBlock.voltage)
                        val powerDelivered = min(loadResult.finalPower, powerLeft)
                        if (loadResult.finalVoltage < consumerBlock.voltageRange.firstDouble() || loadResult.finalVoltage > consumerBlock.voltageRange.secondDouble()) {
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

            if (powerLeft roughlyEquals 0.0 && consumed roughlyEquals consumerBlock.requiredPower) {
                RebarElectricBlock.Consumer.setPowered(consumer.consumerBlock, true)
            }
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
                if (Edge(current, neighbor) in disconnectedEdges || Edge(neighbor, current) in disconnectedEdges) continue
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
        initialPower: Double,
        initialVoltage: Double
    ): LoadResult {
        val loads = existingLoads.toMutableMap()
        var currentPower = initialPower
        var currentVoltage = initialVoltage
        for (edge in path) {
            val remainingCapacity = limits[edge]!! - (loads[edge] ?: 0.0)
            val current = min(currentPower / currentVoltage, remainingCapacity)
            loads.merge(edge, current, Double::plus)
            currentPower = current * currentVoltage
        }
        return LoadResult(loads, currentPower, currentVoltage)
    }

    private data class LoadResult(val currents: Map<Edge, Double>, val finalPower: Double, val finalVoltage: Double)

    private fun recalculateDistanceHeuristics() {
        heuristics.clear()
        for (consumer in consumers) {
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
                    if (neighbor !in visited) {
                        queue.add(neighbor to distance + 1)
                    }
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
                merged.blocks.putAll(network1.blocks)
                merged.blocks.putAll(network2.blocks)
                return merged
            } else {
                return null
            }
        }
    }
}

infix fun Double.roughlyEquals(other: Double): Boolean = abs(this - other) < 1e-6