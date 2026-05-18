package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.electricity.ElectricNode
import io.github.pylonmc.rebar.event.RebarBlockPlaceEvent
import io.github.pylonmc.rebar.util.Vectors
import io.github.pylonmc.rebar.util.position.position
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.jetbrains.annotations.ApiStatus

/**
 * Convenience interface for electric blocks that only have a single producer node
 */
interface RebarElectricProducerBlock : RebarElectricBlock, RebarDirectionalBlock {

    @get:ApiStatus.NonExtendable
    val node: ElectricNode.Producer
        get() = electricNodes.single() as ElectricNode.Producer

    @get:ApiStatus.NonExtendable
    @set:ApiStatus.NonExtendable
    var power: Double
        get() = node.power
        set(value) {
            node.power = value
        }

    val portFace: BlockFace
        get() = facing

    val portRadius: Double
        get() = 0.5

    @ApiStatus.Internal
    companion object : Listener {

        @EventHandler
        private fun onPlace(event: RebarBlockPlaceEvent) {
            val block = event.rebarBlock as? RebarElectricProducerBlock ?: return
            val blockPos = event.block.position
            block.addElectricPort(
                block.portFace,
                ElectricNode.Producer(
                    name = "main",
                    block = blockPos,
                    power = 0.0
                ),
                block.portRadius,
                Vectors.zero,
                null
            )
        }
    }
}