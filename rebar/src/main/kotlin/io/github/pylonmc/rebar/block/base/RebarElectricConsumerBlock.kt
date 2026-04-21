package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.electricity.ElectricNode
import io.github.pylonmc.rebar.electricity.VoltageRange
import io.github.pylonmc.rebar.event.RebarBlockPlaceEvent
import io.github.pylonmc.rebar.util.position.position
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.jetbrains.annotations.ApiStatus

/**
 * Convenience interface for electric blocks that only have a single consumer node
 */
interface RebarElectricConsumerBlock : RebarElectricBlock, RebarDirectionalBlock {

    @get:ApiStatus.NonExtendable
    val node: ElectricNode.Consumer
        get() = electricNodes.single() as ElectricNode.Consumer

    @get:ApiStatus.NonExtendable
    val isPowered: Boolean
        get() = node.isPowered

    val voltageRange: VoltageRange
    val requiredPower: Double

    @ApiStatus.Internal
    companion object : Listener {

        @EventHandler
        private fun onPlace(event: RebarBlockPlaceEvent) {
            val block = event.rebarBlock as? RebarElectricConsumerBlock ?: return
            val blockPos = event.block.position
            block.addElectricPort(
                block.facing,
                ElectricNode.Consumer(
                    block = blockPos,
                    voltageRange = block.voltageRange,
                    requiredPower = block.requiredPower
                )
            )
        }
    }
}