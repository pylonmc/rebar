package io.github.pylonmc.rebar.electricity

import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.block.base.RebarElectricBlock
import java.util.PriorityQueue
import java.util.UUID
import kotlin.collections.ArrayDeque
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableMap
import kotlin.collections.Set
import kotlin.collections.any
import kotlin.collections.asReversed
import kotlin.collections.associateWith
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.contains
import kotlin.collections.getOrPut
import kotlin.collections.isNotEmpty
import kotlin.collections.iterator
import kotlin.collections.last
import kotlin.collections.listOf
import kotlin.collections.mapOf
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.collections.plus
import kotlin.collections.set
import kotlin.collections.sum
import kotlin.collections.toList
import kotlin.collections.toMutableMap
import kotlin.collections.toSet
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
        // First, we distribute power from producers to consumers
        val powerProduced = producers.associateWith { it.producerBlock.power }.toMutableMap()
        var totalPowerProduced = powerProduced.values.sum()
        val powerRequired =
            consumers.associateWith { it.consumerBlock.requiredPower }.toMutableMap()
        val powerSupplied = mutableMapOf<ElectricNode, Double>() // Final amount of power supplied to each consumer

        for (consumer in consumers) {
            RebarElectricBlock.Consumer.setPowered(consumer.consumerBlock, false)
        }

        // I can barely read this loop myself after only 3 weeks but what I *think* it does is distribute power evenly from producers to consumers
        // by evenly "filling up" the required power of each consumer. Any excess is redistributed until we either run out of power or all consumers are fully supplied.
        while (powerRequired.isNotEmpty() && !(totalPowerProduced roughlyEquals 0.0)) {
            val provided = totalPowerProduced / powerRequired.size
            for ((consumer, required) in powerRequired.toList()) {
                val supplied = min(required, provided)
                powerSupplied.merge(consumer, supplied, Double::plus)
                totalPowerProduced -= supplied
                if (supplied >= required) {
                    powerRequired.remove(consumer)
                } else {
                    powerRequired[consumer] = required - supplied
                }
            }
        }

        val limits = mutableMapOf<Edge, Double>()
        var edgeLoads = mapOf<Edge, Double>()
        val disconnectedEdges = mutableSetOf<Edge>()
        for ((consumer, power) in powerSupplied) {
            var powerLeft = power
            val block = consumer.consumerBlock
            while (!(powerLeft roughlyEquals 0.0) && powerProduced.isNotEmpty()) {
                val powerFractionToDistribute = powerLeft / powerProduced.size
                var noPath = 0
                val originalAvailableProducers = powerProduced.size
                producerLoop@ for ((producer, produced) in powerProduced.toList()) {
                    if (produced roughlyEquals 0.0) continue
                    val powerToSupply = min(min(produced, powerFractionToDistribute), powerLeft)
                    val producerBlock = producer.producerBlock
                    val tempDisconnectedEdges = mutableSetOf<Edge>()
                    while (true) {
                        val path = findBestPath(producer, consumer, disconnectedEdges + tempDisconnectedEdges)
                        if (path == null) {
                            noPath++
                            continue@producerLoop
                        }

                        // Determine limits on edges in path if not already known
                        for (edge in path) {
                            if (edge in limits) continue
                            val block = edge.from.maybeConnectorBlock ?: edge.to.maybeConnectorBlock ?: continue
                            limits[edge] = block.getCurrentLimit(edge.to)
                        }

                        val loadResult = calculateLoadOnEdges(
                            path,
                            edgeLoads,
                            limits,
                            powerToSupply,
                            producerBlock.voltage
                        )
                        if (loadResult.finalVoltage > block.voltageRange.secondDouble() || loadResult.finalVoltage < block.voltageRange.firstDouble()) {
                            // Voltage is out of range for this consumer, disconnect the last edge in this path and try again
                            tempDisconnectedEdges.add(path.last())
                        } else {
                            // Update loads on edges and remaining power to supply
                            edgeLoads = loadResult.currents
                            powerLeft -= loadResult.finalPower

                            for (edge in path) {
                                if (edgeLoads[edge]!! >= limits[edge]!!) {
                                    // This edge is saturated, disconnect it
                                    disconnectedEdges.add(edge)
                                }
                            }
                            break
                        }
                    }

                    val remaining = produced - powerToSupply
                    if (remaining roughlyEquals 0.0) {
                        powerProduced.remove(producer)
                    } else {
                        powerProduced[producer] = produced - powerToSupply
                    }
                }

                if (noPath == originalAvailableProducers) {
                    // No producers can supply this consumer, break to avoid infinite loop
                    break
                }
            }

            if (powerLeft roughlyEquals 0.0 && power roughlyEquals block.requiredPower) {
                RebarElectricBlock.Consumer.setPowered(block, true)
            }
        }
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