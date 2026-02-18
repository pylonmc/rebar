package io.github.pylonmc.rebar.content.fluid

import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.block.base.RebarBreakHandler
import io.github.pylonmc.rebar.block.base.RebarEntityHolderBlock
import io.github.pylonmc.rebar.block.context.BlockBreakContext
import io.github.pylonmc.rebar.block.context.BlockBreakContext.PlayerBreak
import io.github.pylonmc.rebar.block.context.BlockCreateContext
import io.github.pylonmc.rebar.i18n.RebarArgument
import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.waila.WailaDisplay
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer

/**
 * An invisible block (structure block) that exists purely to represent a pipe and prevent
 * blocks from being placed on top of them.
 * TODO: [io.github.pylonmc.rebar.block.base.RebarEntityGroupCulledBlock]
 */
class FluidSectionMarker : RebarBlock, RebarBreakHandler, RebarEntityHolderBlock {
    override var disableBlockTextureEntity = true

    @Suppress("unused")
    constructor(block: Block, context: BlockCreateContext) : super(block)

    @Suppress("unused")
    constructor(block: Block, pdc: PersistentDataContainer) : super(block)

    val pipeDisplay
        get() = getHeldRebarEntity(FluidPipeDisplay::class.java, "pipe")

    val pipe
        get() = pipeDisplay?.pipe

    override fun onBreak(drops: MutableList<ItemStack>, context: BlockBreakContext) {
        var player: Player? = null
        if (context is PlayerBreak) {
            player = context.event.player
        }

        pipeDisplay?.delete(player, drops)
    }

    override fun getWaila(player: Player): WailaDisplay?
        = WailaDisplay(defaultWailaTranslationKey.arguments(RebarArgument.of("pipe", pipe!!.stack.effectiveName())))

    override fun getDropItem(context: BlockBreakContext) = null

    override fun getPickItem() = pipe!!.stack

    companion object {
        val KEY = rebarKey("fluid_pipe_section_marker")
    }
}
