package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.block.interfaces.ElectricRebarBlock
import io.github.pylonmc.rebar.electricity.ElectricNode
import io.github.pylonmc.rebar.util.position.position
import org.bukkit.block.BlockFace
import org.jetbrains.annotations.ApiStatus

/**
 * In a [SimpleElectricRebarBlock], all electric nodes created are connected to each other. This allows the abstraction of the concept
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
     * Creates a port of the given [type] and on the given [face]. If you wish to customize the port further, create an [ElectricNode] and call [addElectricPort] directly instead.
     */
    @ApiStatus.NonExtendable
    fun createSimpleElectricPort(type: NodeType, face: BlockFace) {
        val node = when (type) {
            NodeType.CONNECTOR -> ElectricNode.Connector(
                "connector_${electricNodes.count { it is ElectricNode.Connector }}",
                block.position
            )

            NodeType.PRODUCER -> ElectricNode.Producer(
                "producer_${electricNodes.count { it is ElectricNode.Producer }}",
                block.position,
                0.0
            )

            NodeType.CONSUMER -> ElectricNode.Consumer(
                "consumer_${electricNodes.count { it is ElectricNode.Consumer }}",
                block.position,
                0.0
            )
        }
        addElectricPort(ElectricRebarBlock.ElectricPort(node, face))
    }

    @ApiStatus.NonExtendable
    override fun <T : ElectricNode> addElectricNode(node: T): T {
        val node = super.addElectricNode(node)
        for (otherNode in electricNodes) {
            if (otherNode != node) {
                node.connect(otherNode)
            }
        }
        return node
    }

    var requiredPower: Double
        /**
         * @throws IllegalStateException if this block does not have a consumer node
         */
        get() {
            val node = getElectricNode("consumer_0") as? ElectricNode.Consumer
                ?: throw IllegalStateException("Block at ${block.position} does not have a consumer node")
            return node.requiredPower
        }
        /**
         * @throws IllegalStateException if this block does not have a consumer node
         */
        set(value) {
            val node = getElectricNode("consumer_0") as? ElectricNode.Consumer
                ?: throw IllegalStateException("Block at ${block.position} does not have a consumer node")
            node.requiredPower = value
        }

    val isPowered: Boolean
        /**
         * @throws IllegalStateException if this block does not have a consumer node
         */
        get() {
            val node = getElectricNode("consumer_0") as? ElectricNode.Consumer
                ?: throw IllegalStateException("Block at ${block.position} does not have a consumer node")
            return node.isPowered
        }

    var powerProduced: Double
        /**
         * @throws IllegalStateException if this block does not have a producer node
         */
        get() {
            val node = getElectricNode("producer_0") as? ElectricNode.Producer
                ?: throw IllegalStateException("Block at ${block.position} does not have a producer node")
            return node.power
        }
        /**
         * @throws IllegalStateException if this block does not have a producer node
         */
        set(value) {
            val node = getElectricNode("producer_0") as? ElectricNode.Producer
                ?: throw IllegalStateException("Block at ${block.position} does not have a producer node")
            node.power = value
        }

    enum class NodeType {
        CONNECTOR,
        PRODUCER,
        CONSUMER
    }
}