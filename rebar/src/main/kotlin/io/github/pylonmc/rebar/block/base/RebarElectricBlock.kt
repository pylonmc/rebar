package io.github.pylonmc.rebar.block.base

import com.jogamp.common.util.WeakIdentityHashMap
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.electricity.ElectricNode
import io.github.pylonmc.rebar.electricity.ElectricityManager
import io.github.pylonmc.rebar.event.RebarBlockBreakEvent
import io.github.pylonmc.rebar.event.RebarBlockDeserializeEvent
import io.github.pylonmc.rebar.event.RebarBlockSerializeEvent
import io.github.pylonmc.rebar.event.RebarBlockUnloadEvent
import io.github.pylonmc.rebar.util.position.position
import io.github.pylonmc.rebar.util.rebarKey
import it.unimi.dsi.fastutil.doubles.DoubleDoublePair
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.jetbrains.annotations.ApiStatus
import java.util.*

sealed interface RebarElectricBlock {

    // Automatically implemented by RebarBlock
    val block: Block

    @ApiStatus.NonExtendable
    fun createElectricNode(node: ElectricNode): ElectricNode {
        electricBlocks.getOrPut(this, ::mutableListOf).add(node)
        ElectricityManager.addNode(node)
        return node
    }

    @ApiStatus.NonExtendable
    fun createElectricNode(connectionPoint: Location, type: ElectricNode.Type): ElectricNode {
        return createElectricNode(ElectricNode(connectionPoint, block.position, type))
    }

    @ApiStatus.NonExtendable
    fun getElectricNodes(): List<ElectricNode> = electricBlocks[this].orEmpty()

    interface Producer : RebarElectricBlock {

        /**
         * The voltage at which this producer outputs power, in volts
         */
        val voltage: Double

        /**
         * The amount of power this producer is currently outputting, in watts
         */
        val power: Double
    }

    interface Consumer : RebarElectricBlock {

        /**
         * The allowed voltage range for this consumer, as a [DoubleDoublePair] of (minVoltage, maxVoltage).
         * If the voltage supplied to this consumer is outside of this range, [isPowered] will return false.
         */
        val voltageRange: DoubleDoublePair

        /**
         * The amount of power this consumer requires to be powered, in watts.
         * If the supplied power is less than this, [isPowered] will return false.
         */
        val requiredPower: Double

        @get:ApiStatus.NonExtendable
        val isPowered: Boolean
            get() = powered[this] ?: false

        companion object {
            private val powered = WeakIdentityHashMap<RebarElectricBlock, Boolean>()

            @JvmSynthetic
            internal fun setPowered(block: RebarElectricBlock, powered: Boolean) {
                this.powered[block] = powered
            }
        }
    }

    interface Acceptor : RebarElectricBlock {

        /**
         * The allowed voltage range for this acceptor, as a [DoubleDoublePair] of (minVoltage, maxVoltage).
         * If the voltage supplied to this acceptor is outside of this range, it will not accept power and [powerSupplied] will not be called.
         */
        val voltageRange: DoubleDoublePair

        /**
         * The amount of power this acceptor is currently requesting, in watts
         */
        val powerRequested: Double

        /**
         * Called when this acceptor is supplied with power. The amount of power supplied will be less than or equal to [powerRequested].
         */
        fun powerSupplied(power: Double)
    }

    interface Connector : RebarElectricBlock {

        /**
         * Returns the maximum amount of current, in amperes, that can flow from this connector to the given node
         */
        fun getCurrentLimit(otherNode: ElectricNode): Double
    }

    companion object : Listener {

        private val nodesKey = rebarKey("nodes")
        private val nodesType = RebarSerializers.LIST.listTypeFrom(ElectricNode.PDC_TYPE)

        private val electricBlocks = IdentityHashMap<RebarElectricBlock, MutableList<ElectricNode>>()

        @EventHandler
        private fun onDeserialize(event: RebarBlockDeserializeEvent) {
            val block = event.rebarBlock
            if (block !is RebarElectricBlock) return
            val nodes = event.pdc.get(nodesKey, nodesType)!!.toMutableList()
            electricBlocks[block] = nodes

            for (node in nodes) {
                ElectricityManager.addNode(node)
            }
        }

        @EventHandler
        private fun onSerialize(event: RebarBlockSerializeEvent) {
            val block = event.rebarBlock
            if (block !is RebarElectricBlock) return
            event.pdc.set(nodesKey, nodesType, electricBlocks[block].orEmpty())
        }

        @EventHandler
        private fun onUnload(event: RebarBlockUnloadEvent) {
            if (event.rebarBlock is RebarElectricBlock) {
                for (node in electricBlocks.remove(event.rebarBlock)!!) {
                    ElectricityManager.removeNode(node)
                }
            }
        }

        @EventHandler
        private fun onBreak(event: RebarBlockBreakEvent) {
            if (event.rebarBlock is RebarElectricBlock) {
                for (node in electricBlocks.remove(event.rebarBlock)!!) {
                    for (connection in node.connections) {
                        ElectricityManager.getNodeById(connection)?.disconnect(node)
                    }
                    ElectricityManager.removeNode(node)
                }
            }
        }
    }
}