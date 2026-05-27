package io.github.pylonmc.rebar.block.context

import io.github.pylonmc.rebar.entity.display.transform.TransformUtil
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

/**
 * Information surrounding a block place event.
 */
interface BlockCreateContext {

    /**
     * The player who placed/caused the block to be placed, if applicable
     */
    val player: Player?

    /**
     * The direction in which this block was placed. NORTH, EAST, SOUTH, WEST.
     */
    val facing: BlockFace

    /**
     * The direction in which this block was placed. NORTH, EAST, SOUTH, WEST, UP, DOWN
     */
    val facingVertical: BlockFace

    /**
     * The old block where the new block is about to be created.
     */
    val block: Block

    /**
     * The item used to create the block, if applicable.
     */
    val item: ItemStack?

    /**
     * If true, the type of the block will be set to the type of the Rebar block
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("shouldSetType")
    val shouldSetType: Boolean
        get() = true

    /**
     * A player has placed the block
     */
    data class PlayerPlace(
        override val item: ItemStack,
        val event: BlockPlaceEvent
    ) : BlockCreateContext {
        override val player: Player = event.player
        override val facing: BlockFace = TransformUtil.yawToCardinalFace(player.yaw)
        override val facingVertical: BlockFace = TransformUtil.yawAndPitchToFace(player.yaw, player.pitch)
        override val block = event.blockPlaced
        override val shouldSetType = false // The action of the placement sets the block
    }

    /**
     * A plugin generated the block
     * ex:
     * - Growing of Rebar Trees
     * - Evolution of Rebar Sponges
     * - Spread of Rebar Fire
     */
    @JvmRecord
    data class PluginGenerate(
        val plugin: Plugin,
        override val player: Player? = null,
        override val facing: BlockFace = BlockFace.NORTH,
        override val facingVertical: BlockFace = BlockFace.NORTH,
        override val block: Block,
        override val item: ItemStack? = null,
        override val shouldSetType: Boolean = true,
    ) : BlockCreateContext

    /**
     * A context in which no other reason is specified
     */
    @JvmRecord
    data class Default @JvmOverloads constructor(
        override val player: Player? = null,
        override val block: Block,
        override val facing: BlockFace = BlockFace.NORTH,
        override val facingVertical: BlockFace = BlockFace.NORTH,
        override val item: ItemStack? = null,
        override val shouldSetType: Boolean = true
    ) : BlockCreateContext

    @JvmRecord
    data class ManualLoading @JvmOverloads constructor(
        override val player: Player? = null,
        override val block: Block,
        override val facing: BlockFace = BlockFace.NORTH,
        override val facingVertical: BlockFace = BlockFace.NORTH,
        override val item: ItemStack? = null,
        override val shouldSetType: Boolean = true
    ) : BlockCreateContext
}