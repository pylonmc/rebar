package io.github.pylonmc.rebar.electricity.nodes

import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.util.Vector

/**
 * Holds information for constructing an [ElectricPortEntity]
 */
@JvmRecord
data class ElectricPortSpec @JvmOverloads constructor(
    val node: ElectricNode,
    val face: BlockFace,
    val radius: Double = 0.5,
    val offset: Vector = Vector(0.0, 0.0, 0.0),
    val material: Material = when (node) {
        is ElectricConnectorNode -> Material.GRAY_CONCRETE
        is ElectricConsumerNode, is ElectricAcceptorNode -> Material.LIME_CONCRETE
        is ElectricProducerNode -> Material.RED_CONCRETE
    }
) {
    fun radius(radius: Double) = copy(radius = radius)
    fun offset(offset: Vector) = copy(offset = offset)
    fun material(material: Material) = copy(material = material)
}