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
 * Convenience interface for electric blocks that only have a single consumer node
 */
interface RebarElectricConsumerBlock : RebarElectricBlock, RebarDirectionalBlock {

    @get:ApiStatus.NonExtendable
    val node: ElectricNode.Consumer
        get() = electricNodes.single() as ElectricNode.Consumer

    @get:ApiStatus.NonExtendable
    val isPowered: Boolean
        get() = node.isPowered

    @get:ApiStatus.NonExtendable
    @set:ApiStatus.NonExtendable
    var requiredPower: Double
        get() = node.requiredPower
        set(value) {
            node.requiredPower = value
        }

    val portFace: BlockFace
        get() = facing

    val portRadius: Double
        get() = 0.5

    @ApiStatus.Internal
    companion object : Listener {

        @EventHandler
        private fun onPlace(event: RebarBlockPlaceEvent) {
            val block = event.rebarBlock as? RebarElectricConsumerBlock ?: return
            val blockPos = event.block.position
            block.addElectricPort(
                block.portFace,
                ElectricNode.Consumer(
                    name = "main",
                    block = blockPos,
                    requiredPower = 0.0
                ),
                block.portRadius,
                Vectors.zero,
                null
            )
        }
    }
}