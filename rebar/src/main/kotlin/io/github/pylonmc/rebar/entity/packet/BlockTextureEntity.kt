package io.github.pylonmc.rebar.entity.packet

import io.github.pylonmc.rebar.block.RebarBlock
import org.bukkit.Color
import org.bukkit.entity.Display
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Transformation
import org.joml.Matrix4f
import java.util.UUID

interface BlockTextureEntity {
    val block: RebarBlock
    var id: Int
    var uuid: UUID
    val viewers: Set<UUID>

    val isSpawned: Boolean

    var transformation: Transformation
    var transformationMatrix: Matrix4f

    var interpolationDelay: Int
    var interpolationDuration: Int
    var teleportDuration: Int

    var shadowRadius: Float
    var shadowStrength: Float

    var displayWidth: Float
    var displayHeight: Float
    var viewRange: Float

    var glowColorOverride: Color?
    var billboard: Display.Billboard
    var brightness: Display.Brightness?

    var itemStack: ItemStack?
    var itemDisplayTransform: ItemDisplay.ItemDisplayTransform

    fun addOrRefreshViewer(playerId: UUID, distanceSquared: Double)
    fun refreshViewer(playerId: UUID, distanceSquared: Double)
    fun hasViewer(playerId: UUID): Boolean
    fun removeViewer(playerId: UUID)
    fun removeAllViewers()

    fun spawn()

    companion object {
        /**
         * A base scale to prevent z-fighting between the block and item display when the player is right next to a block.
         */
        const val BLOCK_OVERLAP_INCREASE = 0.0005f

        /**
         * The maximum scale increase to prevent the item display from becoming too large at long distances.
         */
        const val MAX_SCALE_INCREASE = 0.1f

        /**
         * The problem with making the item display too large is that the block break overlay will be obscured by the item display.
         * This issue is mainly only prevalent when close to the block, so the farther away they are from the block the more we can
         * increase the scale to prevent z-fighting without worrying about the block break overlay being obscured.
         */
        const val DOUBLE_OVERLAP_INCREASE_DISTANCE = 3

        /**
         * Calculate scale increase so that at EXPECTED_REACH_DISTANCE, the scale increase is double BLOCK_OVERLAP_SCALE
         */
        const val SCALE_DISTANCE_INCREASE =
            BLOCK_OVERLAP_INCREASE / (DOUBLE_OVERLAP_INCREASE_DISTANCE * DOUBLE_OVERLAP_INCREASE_DISTANCE)

        /**
         * If the difference between the new scale increase and the last scale increase sent to the viewer is less than 1/10th of the base BLOCK_OVERLAP_SCALE, we don't send a packet to avoid unnecessary packet spam.
         */
        const val SCALE_DISTANCE_THRESHOLD = BLOCK_OVERLAP_INCREASE / 10.0f
    }
}