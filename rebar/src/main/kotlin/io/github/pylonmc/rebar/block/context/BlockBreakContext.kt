package io.github.pylonmc.rebar.block.context

import com.destroystokyo.paper.event.block.BlockDestroyEvent
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.content.debug.DebugWaxedWeatheredCutCopperStairs
import org.bukkit.GameMode
import org.bukkit.block.Block
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockFadeEvent
import org.bukkit.event.entity.EntityExplodeEvent

/**
 * Information surrounding a block break event. Used to centralize block breaking logic so that
 * [BlockBreakEvent], [BlockExplodeEvent], etc. can all be references by one interface rather than
 * having to be dealt with individually.
 */
interface BlockBreakContext {

    /**
     * The block that is about to be broken.
     */
    val block: Block

    /**
     * Determines the default drop behavior of the context
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("normallyDrops")
    val normallyDrops: Boolean

    /**
     * If true, the block will be set to air after the Rebar data is removed
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("shouldSetToAir")
    val shouldSetToAir: Boolean
        get() = true

    /**
     * The block is being forcefully broken by [BlockStorage.deleteBlock]
     *
     * See [DebugWaxedWeatheredCutCopperStairs] for example
     */
    class Delete(override val block: Block) : BlockBreakContext {
        override val normallyDrops = false
        override val shouldSetToAir = true
    }

    /**
     * The block is being broken by a plugin
     */
    class PluginBreak @JvmOverloads constructor(
        override val block: Block,
        override val normallyDrops: Boolean = true,
        override val shouldSetToAir: Boolean = true,
    ) : BlockBreakContext

    /**
     * The block is being broken by a player
     */
    @JvmRecord
    data class PlayerBreak(val event: BlockBreakEvent) : BlockBreakContext {
        override val block
            get() = event.block
        override val normallyDrops
            get() = event.player.gameMode != GameMode.CREATIVE
    }

    /**
     * The block is being exploded by an entity (e.g. a creeper or TNT)
     */
    data class EntityExploded(override val block: Block, val event: EntityExplodeEvent) : BlockBreakContext {
        override val normallyDrops = true
    }

    /**
     * The block is exploding (e.g. beds in the Nether/End, respawn anchors in the Overworld/End)
     */
    data class BlockExplosionOrigin(val event: BlockExplodeEvent) : BlockBreakContext {
        override val block = event.block
        override val normallyDrops = false
    }

    /**
     * The block is being exploded as a result of another block exploding
     */
    data class BlockExploded(override val block: Block, val event: BlockExplodeEvent) : BlockBreakContext {
        override val normallyDrops
            get() = Math.random() <= event.yield
    }

    /**
     * The block has been burned away by fire
     */
    data class Burned(val event: BlockBurnEvent) : BlockBreakContext {
        override val block = event.block
        override val normallyDrops = false
    }

    /**
     * The block has faded away (see [BlockFadeEvent] for more details)
     */
    data class Faded(val event: BlockFadeEvent) : BlockBreakContext {
        override val block = event.block
        override val normallyDrops = false
    }

    /**
     * The block has been destroyed for another reason, such as a lever dropping
     * because the block it was attached to was broken. It is not really documented
     * exactly when the event that this relies on is actually fired for some reason.
     *
     * Unfortunate quirk of Paper/Spigot/Bukkit/Mojang that this is needed.
     *
     * See [BlockDestroyEvent] for more details
     */
    data class Destroyed(val event: BlockDestroyEvent) : BlockBreakContext {
        override val block = event.block
        override val normallyDrops = event.willDrop()
    }
}