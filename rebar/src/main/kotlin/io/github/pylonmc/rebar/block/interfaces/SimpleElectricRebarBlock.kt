package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.electricity.nodes.*
import io.github.pylonmc.rebar.util.position.position
import org.bukkit.block.BlockFace
import org.jetbrains.annotations.ApiStatus

/**
 * In a [SimpleElectricRebarBlock], all electric nodes created are connected to a central "master" connector node. This allows the abstraction of the concept
 * of "nodes" into a general "electric block" that can have any number of connectors, producers, and consumers without needing to worry
 * about full interactions, while also providing simple utility methods for interacting with the electricity system.
 *
 * Each node is named consecutively by its type starting from 0. For example, if you create two producer nodes and one consumer node,
 * they will be named "producer_0", "producer_1", and "consumer_0", respectively. All interaction methods/properties will only interact
 * with the zeroth node of each type, so in this example, the "producer_0" and "consumer_0" nodes. Since all nodes in a block are interconnected,
 * this means the "producer_1" node produces 0 power by itself, but allows power to flow from "producer_0" into itself, thereby allowing
 * it to power other blocks as well.
 */
interface SimpleElectricRebarBlock : ElectricRebarBlock {

    /**
     * Creates a port of the given [type], on the given [face] and with the given [radius].
     * If you wish to customize the port further, create an [ElectricNode] and call [addElectricPort] directly instead.
     */
    @ApiStatus.NonExtendable
    fun createSimpleElectricPort(type: ElectricNodeType, face: BlockFace, radius: Double) {
        val node = when (type) {
            ElectricNodeType.CONNECTOR -> ElectricConnectorNode(
                "connector_${electricNodes.count { it is ElectricConnectorNode }}",
                block.position
            )

            ElectricNodeType.PRODUCER -> ElectricProducerNode(
                "producer_${electricNodes.count { it is ElectricProducerNode }}",
                block.position,
                0.0
            )

            ElectricNodeType.CONSUMER -> ElectricConsumerNode(
                "consumer_${electricNodes.count { it is ElectricConsumerNode }}",
                block.position,
                0.0
            )

            ElectricNodeType.ACCEPTOR -> ElectricAcceptorNode(
                "acceptor_${electricNodes.count { it is ElectricAcceptorNode }}",
                block.position,
            )
        }
        addElectricPort(ElectricPortSpec(node, face, radius = radius))
    }

    /**
     * Creates a port of the given [type], on the given [face]
     */
    @ApiStatus.NonExtendable
    fun createSimpleElectricPort(type: ElectricNodeType, face: BlockFace) = createSimpleElectricPort(type, face, 0.5)

    @ApiStatus.NonExtendable
    override fun <T : ElectricNode> addElectricNode(node: T): T {
        val node = super.addElectricNode(node)
        val masterNode = getElectricNode<ElectricConnectorNode>(MASTER)
            ?: super.addElectricNode(ElectricConnectorNode(MASTER, block.position))
        node.connect(masterNode)
        return node
    }

    val hasConsumerNodes: Boolean
        get() = getElectricNode(DEFAULT_CONSUMER) != null

    val hasProducerNodes: Boolean
        get() = getElectricNode(DEFAULT_PRODUCER) != null

    var requiredPower: Double
        /**
         * Returns 0 if this block does not have a consumer node
         */
        get() = getElectricNode<ElectricConsumerNode>(DEFAULT_CONSUMER)?.requiredPower ?: 0.0
        /**
         * @throws IllegalStateException if this block does not have a consumer node
         */
        set(value) {
            val node = getElectricNode<ElectricConsumerNode>(DEFAULT_CONSUMER)
                ?: throw IllegalStateException("Block at ${block.position} does not have a consumer node")
            node.requiredPower = value
        }

    val isPowered: Boolean
        /**
         * @throws IllegalStateException if this block does not have a consumer node
         */
        get() {
            val node = getElectricNode<ElectricConsumerNode>(DEFAULT_CONSUMER)
                ?: throw IllegalStateException("Block at ${block.position} does not have a consumer node")
            return node.isPowered
        }

    var powerProduced: Double
        /**
         * Returns 0 if this block does not have a producer node
         */
        get() = getElectricNode<ElectricProducerNode>(DEFAULT_PRODUCER)?.power ?: 0.0
        /**
         * @throws IllegalStateException if this block does not have a producer node
         */
        set(value) {
            val node = getElectricNode<ElectricProducerNode>(DEFAULT_PRODUCER)
                ?: throw IllegalStateException("Block at ${block.position} does not have a producer node")
            node.power = value
        }

    companion object {
        private const val DEFAULT_PRODUCER = "producer_0"
        private const val DEFAULT_CONSUMER = "consumer_0"
        private const val MASTER = "master"
    }
}