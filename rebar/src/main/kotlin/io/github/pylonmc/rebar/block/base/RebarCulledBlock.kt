package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.culling.BlockCullingEngine
import io.github.pylonmc.rebar.culling.BlockCullingEngine.isVisibilityInverted
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Enables a [RebarBlock] to take advantage of the [BlockCullingEngine]
 * to do something per player when the block is considered culled or visible.
 */
interface RebarCulledBlock {
    /**
     * If [onVisible] and [onCulled] should be called asynchronously instead of scheduled to be called on the main thread.
     */
    val isCulledAsync: Boolean
        get() = false

    /**
     * If the block is currently visible to the player.
     * Used only to determine which [io.github.pylonmc.rebar.culling.PlayerCullingConfig] interval to use
     */
    fun isVisible(player: Player): Boolean

    /**
     * What to do when the block is considered visible to the player.
     * This method will be called regardless of if the block was already considered visible
     * So you should still run checks in this method to prevent doing unnecessary work.
     */
    fun onVisible(player: Player)

    /**
     * What to do when the block is considered culled to the player.
     * This method will be called regardless of if the block was already considered culled
     * So you should still run checks in this method to prevent doing unnecessary work.
     */
    fun onCulled(player: Player)
}