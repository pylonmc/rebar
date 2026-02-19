package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.event.RebarBlockBreakEvent
import io.github.pylonmc.rebar.event.RebarBlockLoadEvent
import io.github.pylonmc.rebar.event.RebarBlockPlaceEvent
import io.github.pylonmc.rebar.event.RebarBlockUnloadEvent
import net.kyori.adventure.text.Component
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.jetbrains.annotations.MustBeInvokedByOverriders
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.invui.window.Window
import java.util.IdentityHashMap

/**
 * A simple interface that opens a GUI when the block is right clicked
 *
 * The title of the window opened is by default the block's name. Override [guiTitle] to change this.
 *
 * See [InvUI docs](https://docs.xenondevs.xyz/invui/) for information on how to make GUIs.
 *
 * @see Gui
 * @see VirtualInventory
 * @see RebarVirtualInventoryBlock
 */
interface RebarGuiBlock : RebarBreakHandler, RebarNoVanillaContainerBlock {

    /**
     * Returns the block's GUI. Called when a block is created.
     */
    fun createGui(): Gui

    /**
     * The title of the GUI
     */
    val guiTitle: Component
        get() = (this as RebarBlock).nameTranslationKey

    companion object : Listener {
        private val guiBlocks = IdentityHashMap<RebarGuiBlock, Gui>()

        @EventHandler
        private fun onPlace(event: RebarBlockPlaceEvent) {
            if (event.rebarBlock is RebarGuiBlock) {
                guiBlocks[event.rebarBlock] = event.rebarBlock.createGui()
            }
        }

        @EventHandler
        private fun onLoad(event: RebarBlockLoadEvent) {
            if (event.rebarBlock is RebarGuiBlock) {
                guiBlocks[event.rebarBlock] = event.rebarBlock.createGui()
            }
        }

        @EventHandler(priority = EventPriority.HIGH)
        private fun onInteract(event: PlayerInteractEvent) {
            val guiBlock = BlockStorage.getAs(RebarGuiBlock::class.java, event.clickedBlock ?: return) ?: return

            if (!event.action.isRightClick
                || event.player.isSneaking
                || event.hand != EquipmentSlot.HAND
                || event.useInteractedBlock() == Event.Result.DENY
            ) {
                return
            }

            event.setUseInteractedBlock(Event.Result.DENY)
            event.setUseItemInHand(Event.Result.DENY)

            Window.builder()
                .setUpperGui(guiBlocks[guiBlock]!!)
                .setTitle(guiBlock.guiTitle)
                .setViewer(event.player)
                .build()
                .open()
        }

        @EventHandler
        private fun onBreak(event: RebarBlockBreakEvent) {
            if (event.rebarBlock is RebarGuiBlock) {
                guiBlocks.remove(event.rebarBlock)!!.closeForAllViewers()
            }
        }

        @EventHandler
        private fun onUnload(event: RebarBlockUnloadEvent) {
            if (event.rebarBlock is RebarGuiBlock) {
                guiBlocks.remove(event.rebarBlock)!!.closeForAllViewers()
            }
        }
    }
}