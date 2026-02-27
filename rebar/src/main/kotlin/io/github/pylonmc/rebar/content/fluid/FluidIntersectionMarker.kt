package io.github.pylonmc.rebar.content.fluid

import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.block.base.RebarBreakHandler
import io.github.pylonmc.rebar.block.base.RebarEntityHolderBlock
import io.github.pylonmc.rebar.block.base.RebarFacadeBlock
import io.github.pylonmc.rebar.block.context.BlockBreakContext
import io.github.pylonmc.rebar.block.context.BlockBreakContext.PlayerBreak
import io.github.pylonmc.rebar.block.context.BlockCreateContext
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.i18n.RebarArgument
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.waila.WailaDisplay
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer

/**
 * A 'fluid pipe connector' is one of the small gray displays that appears
 * on pipe corners/junctions.
 * TODO: [io.github.pylonmc.rebar.block.base.RebarEntityGroupCulledBlock]
 */
class FluidIntersectionMarker : RebarBlock, RebarEntityHolderBlock, RebarBreakHandler, RebarFacadeBlock {
    override val facadeDefaultBlockType = Material.STRUCTURE_VOID
    override var disableBlockTextureEntity = true

    @Suppress("unused")
    constructor(block: Block, context: BlockCreateContext) : super(block) {
        addEntity("intersection", FluidIntersectionDisplay(block))
    }

    @Suppress("unused")
    constructor(block: Block, pdc: PersistentDataContainer) : super(block)

    val fluidIntersectionDisplay
        get() = getHeldRebarEntityOrThrow(FluidIntersectionDisplay::class.java, "intersection")

    override fun onBreak(drops: MutableList<ItemStack>, context: BlockBreakContext) {
        var player: Player? = if (context is PlayerBreak) context.event.player else null

        // Clone to prevent ConcurrentModificationException if pipeDisplay.delete modified connectedPipeDisplays
        for (pipeDisplayId in fluidIntersectionDisplay.connectedPipeDisplays.toSet()) {
            val pipeDisplay = EntityStorage.getAs<FluidPipeDisplay>(pipeDisplayId)
            // can be null if called from two different location (eg two different connection points removing the display)
            pipeDisplay?.delete(player, drops)
        }
    }

    override fun getWaila(player: Player): WailaDisplay?
        = WailaDisplay(defaultWailaTranslationKey.arguments(RebarArgument.of("pipe", this.pipe.stack.effectiveName())))

    val pipe: RebarItem
        get() {
            check(fluidIntersectionDisplay.connectedPipeDisplays.isNotEmpty())
            val uuid = fluidIntersectionDisplay.connectedPipeDisplays.iterator().next()
            return EntityStorage.getAs<FluidPipeDisplay?>(uuid)!!.pipe
        }

    override fun getDropItem(context: BlockBreakContext) = null

    override fun getPickItem() = pipe.stack

    companion object {
        @JvmField
        val KEY = rebarKey("fluid_pipe_intersection_marker")
    }
}
