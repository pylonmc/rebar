package io.github.pylonmc.rebar.electricity

import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.electricity.nodes.*
import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.world.storage
import it.unimi.dsi.fastutil.objects.Object2DoubleMap
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectDoubleMutablePair
import it.unimi.dsi.fastutil.objects.ObjectDoublePair
import org.bukkit.World
import java.util.PriorityQueue
import java.util.UUID
import kotlin.collections.ArrayDeque
import kotlin.math.abs
import kotlin.math.min

/**
 * A set of nodes where each node has at least one connection to another node in the set.
 *
 * See https://pylonmc.github.io/documentation/internals/electricity/#electric-networks for a much more comprehensive
 * technical documentation.
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
    private var distances: Map<ElectricNode, Map<ElectricNode, Int>>? = null

    private var snapshot: ConsumerSnapshot? = null

    fun addNode(node: ElectricNode) {
        nodeMap[node.id] = node
        when (node) {
            is ElectricProducerNode -> producers.add(node)
            is ElectricConsumerNode -> consumers.add(node)
            is ElectricAcceptorNode -> acceptors.add(node)
            is ElectricConnectorNode -> {}
        }
        distances = null
    }

    fun removeNode(node: ElectricNode) {
        nodeMap.remove(node.id)
        producers.remove(node)
        consumers.remove(node)
        acceptors.remove(node)
        distances = null
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
                snapshot.remainingPower.first { it.key() == producer }.value(newPower)
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
        // consumer tick
        if (snapshot == null) {
            distributePowerToConsumers()
        }

        // acceptor tick
        val remainingPower = snapshot!!.remainingPower.map { ObjectDoubleMutablePair(it.key(), it.valueDouble()) }
        val disconnectedEdges = snapshot!!.disconnectedEdges.toMutableSet()
        val edgeLoads = snapshot!!.edgeLoads

        distributePowerToAcceptors(remainingPower, disconnectedEdges, edgeLoads)

        // producer tick
        for ((producer, remaining) in remainingPower) {
            val taken = producer.power * POWER_ADJUSTMENT - remaining
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

        var hasUnpoweredConsumers = false

        val sortedProducers = producers.mapTo(mutableListOf()) { ObjectDoubleMutablePair(it, it.power) }
        sortedProducers.sortWith(
            compareBy<ObjectDoublePair<ElectricProducerNode>> { it.key().priority }
                .thenComparingDouble { -it.key().power }
        )

        var edgeLoads: Object2DoubleMap<Edge> = Object2DoubleOpenHashMap()
        var disconnectedEdges = setOf<Edge>()
        for (consumer in consumers.sortedBy { it.requiredPower }) {
            var powerLeft = consumer.requiredPower
            var currentEdgeLoads: Object2DoubleMap<Edge> = Object2DoubleOpenHashMap(edgeLoads)
            val currentDisconnectedEdges = disconnectedEdges.toMutableSet()
            var iterations = 0
            while (!(powerLeft roughlyEquals 0.0)) {
                if (iterations++ == 100_000) {
                    throw RuntimeException("Infinite loop detected")
                }
                var noPath = 0
                for (pair in sortedProducers) {
                    val (producer, produced) = pair
                    if (produced roughlyEquals 0.0) {
                        noPath++
                        continue
                    }
                    val path = findBestPath(producer, consumer, currentDisconnectedEdges)
                    if (path == null) {
                        noPath++
                        continue
                    }

                    val loadResult = calculateLoadOnEdges(path, currentEdgeLoads, min(produced, powerLeft))
                    val powerDelivered = min(loadResult.finalPower, powerLeft)
                    currentEdgeLoads = loadResult.currents
                    powerLeft -= powerDelivered
                    pair.value(produced - powerDelivered)
                    for ((edge, load) in loadResult.currents) {
                        if (load roughlyEquals edge.powerLimit) {
                            currentDisconnectedEdges.add(edge)
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
                edgeLoads = currentEdgeLoads
                disconnectedEdges = currentDisconnectedEdges
            } else {
                hasUnpoweredConsumers = true
            }
        }

        val surplusProducers = sortedProducers.filter { (producer, power) -> producer.power roughlyEquals power }
            .mapTo(mutableSetOf()) { it.key() }

        snapshot = ConsumerSnapshot(
            sortedProducers,
            disconnectedEdges,
            edgeLoads,
            surplusProducers,
            hasUnpoweredConsumers
        )
    }

    // see https://pylonmc.github.io/documentation/internals/electricity/#consumer-snapshots
    private data class ConsumerSnapshot(
        val remainingPower: List<ObjectDoublePair<ElectricProducerNode>>,
        val disconnectedEdges: Set<Edge>,
        val edgeLoads: Object2DoubleMap<Edge>,
        val surplusProducers: Set<ElectricProducerNode>,
        val hasUnpoweredConsumers: Boolean,
    )

    private fun distributePowerToAcceptors(
        surplusPower: List<ObjectDoublePair<ElectricProducerNode>>,
        disconnectedEdges: MutableSet<Edge>,
        edgeLoads: Object2DoubleMap<Edge>
    ) {
        var edgeLoads = edgeLoads
        do {
            var notAccepted = 0
            acceptorLoop@ for (acceptor in acceptors) {
                var remaining = surplusPower.sumOf { it.valueDouble() } / acceptors.size
                if (remaining roughlyEquals 0.0) {
                    notAccepted++
                    continue
                }
                var noPath = 0
                for (pair in surplusPower) {
                    val (producer, surplus) = pair
                    if (surplus roughlyEquals 0.0) {
                        noPath++
                        continue
                    }
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
                            pair.value(surplus - accepted)
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
     * Calculates the load for edges based on a greedy best first search from the producer to the consumer, using the graph distance to consumers as the heuristic.
     *
     * Implementation note: Seggan would've used A*, but in this case, since the heuristic is perfect, greedy best first search is more efficient.
     */
    private fun findBestPath(
        producer: ElectricNode,
        consumer: ElectricNode,
        disconnectedEdges: Set<Edge>,
    ): List<Edge>? {
        if (distances == null) distances = calculateDistanceHeuristics()
        val heuristic =
            distances!![consumer] ?: throw IllegalArgumentException("Target node is not a consumer in this network")
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
            val remainingCapacity = edge.powerLimit - loads.getOrDefault(edge, 0.0)
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

        fun clearData() {
            world.storage.remove(powerLimitKey)
            world.storage.remove(unidirectionalKey)
        }
    }

    companion object {

        private val POWER_ADJUSTMENT = RebarConfig.ELECTRICITY_TICK_INTERVAL / 20.0

        /**
         * @return the merged network if [network1] and [network2] are connected by at least one edge, or null otherwise
         */
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

private operator fun <K> ObjectDoublePair<K>.component1(): K = this.key()
private operator fun ObjectDoublePair<*>.component2(): Double = this.valueDouble()