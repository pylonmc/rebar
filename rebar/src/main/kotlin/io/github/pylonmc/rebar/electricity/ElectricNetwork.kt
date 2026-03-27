package io.github.pylonmc.rebar.electricity

import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.block.base.RebarElectricBlock
import java.util.PriorityQueue
import java.util.UUID
import kotlin.math.min

class ElectricNetwork {

    private val nodeMap: MutableMap<UUID, ElectricNode> = mutableMapOf()
    val nodes: Set<ElectricNode> get() = nodeMap.values.toSet()

    private val producers = mutableSetOf<ElectricNode>()
    private val consumers = mutableSetOf<ElectricNode>()

    private val blocks = mutableMapOf<ElectricNode, RebarElectricBlock>()

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
        blocks[node] = BlockStorage.getAsOrThrow(node.block)
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

    fun tick() {
        // First, we distribute power from producers to consumers
        val powerProduced = producers.associateWith { (blocks[it] as RebarElectricBlock.Producer).power }
        var totalPowerProduced = powerProduced.values.sum()
        val powerRequired =
            consumers.associateWith { (blocks[it] as RebarElectricBlock.Consumer).requiredPower }.toMutableMap()
        val powerSupplied = mutableMapOf<ElectricNode, Double>()
        while (powerRequired.isNotEmpty() && totalPowerProduced > 0) {
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
        existingLoads: MutableMap<Edge, Double>,
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