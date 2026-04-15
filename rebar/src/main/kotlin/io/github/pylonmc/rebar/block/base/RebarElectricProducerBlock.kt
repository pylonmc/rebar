package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.electricity.ElectricNode
import io.github.pylonmc.rebar.event.RebarBlockPlaceEvent
import io.github.pylonmc.rebar.util.position.position
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.jetbrains.annotations.ApiStatus

/**
 * Convenience interface for electric blocks that only have a single producer node
 */
interface RebarElectricProducerBlock : RebarElectricBlock {

    @get:ApiStatus.NonExtendable
    val node: ElectricNode.Producer
        get() = getElectricNodes().single() as ElectricNode.Producer

    @get:ApiStatus.NonExtendable
    @set:ApiStatus.NonExtendable
    var voltageProducing: Double
        get() = node.voltage
        set(value) {
            node.voltage = value
        }

    @get:ApiStatus.NonExtendable
    @set:ApiStatus.NonExtendable
    var powerProducing: Double
        get() = node.power
        set(value) {
            node.power = value
        }

    @ApiStatus.Internal
    companion object : Listener {

        @EventHandler
        private fun onPlace(event: RebarBlockPlaceEvent) {
            val block = event.rebarBlock as? RebarElectricProducerBlock ?: return
            val blockPos = event.block.position
            block.addElectricNode(
                ElectricNode.Producer(
                    block = blockPos,
                    connectionPoint = blockPos.toLocation().toCenterLocation(),
                    voltage = 0.0,
                    power = 0.0
                )
            )
        }
    }
}