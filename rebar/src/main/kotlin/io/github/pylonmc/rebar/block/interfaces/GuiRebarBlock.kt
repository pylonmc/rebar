package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.event.RebarBlockBreakEvent
import io.github.pylonmc.rebar.event.RebarBlockLoadEvent
import io.github.pylonmc.rebar.event.RebarBlockPlaceEvent
import io.github.pylonmc.rebar.event.RebarBlockUnloadEvent
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.invui.window.Window
import java.util.IdentityHashMap

/**
 * A simple interface that opens a GUI when the block is right clicked.
 *
 * To use this interface, simply override [GuiRebarBlock.createGui]
 * to return the GUI you want to be opened when the block is right clicked. Keep in mind that once
 * the GUI has been created for the first time, [GuiRebarBlock.createGui]
 * will not be called again until the block is reloaded.
 *
 * This interface doesn't provide a way to update the GUI once it is created. This is mostly because
 * the vast majority of InvUI GUIs should not need to be completely replaced once created, and instead
 * you can [update individual items](https://docs.xenondevs.xyz/invui/item/) or use something like a
 * [PagedGui](https://docs.xenondevs.xyz/invui/gui/#paged-gui).
 *
 * The title of the window opened is by default the block's name. Override
 * [GuiRebarBlock.guiTitle] to change this.
 *
 * See [InvUI docs](https://docs.xenondevs.xyz/invui/) for information on how to make GUIs.
 *
 * @see Gui
 * @see VirtualInventory
 * @see VirtualInventoryRebarBlock
 */
interface GuiRebarBlock : NoVanillaInventoryRebarBlock {

    /**
     * The title of the GUI
     */
    val guiTitle: Component
        get() = (this as RebarBlock).nameTranslationKey

    /**
     * Returns the block's GUI. Called when a block is created.
     */
    fun createGui(): Gui

    /**
     * Refreshes the stores GUI by calling [createGui] again.
     *
     * If players have the GUI already open, it will be closed and then re-opened with the
     * new GUI manually.
     *
     * Strongly consider [updating individual items](https://docs.xenondevs.xyz/invui/item/)
     * before you use this method, to prevent having to constantly close and re-open windows.
     */
    fun refreshGui() {
        val oldGui = guiBlocks[this]
        val players = oldGui?.windows?.map { it.viewer } ?: emptyList()
        oldGui?.windows?.forEach { it.close() }
        guiBlocks[this] = createGui()
        for (player in players) {
            open(player)
        }
    }

    fun open(player: Player) {
            Window.builder()
                .setUpperGui(guiBlocks[this]!!)
                .setTitle(guiTitle)
                .setViewer(player)
                .build()
                .open()
    }

    companion object : Listener {
        private val guiBlocks = IdentityHashMap<GuiRebarBlock, Gui>()

        @EventHandler
        private fun onPlace(event: RebarBlockPlaceEvent) {
            if (event.rebarBlock is GuiRebarBlock) {
                guiBlocks[event.rebarBlock] = event.rebarBlock.createGui()
            }
        }

        @EventHandler
        private fun onLoad(event: RebarBlockLoadEvent) {
            if (event.rebarBlock is GuiRebarBlock) {
                guiBlocks[event.rebarBlock] = event.rebarBlock.createGui()
            }
        }

        @EventHandler(priority = EventPriority.HIGH)
        private fun onInteract(event: PlayerInteractEvent) {
            val guiBlock = BlockStorage.getAs(GuiRebarBlock::class.java, event.clickedBlock ?: return) ?: return

            if (!event.action.isRightClick
                || (event.player.isSneaking && event.isBlockInHand)
                || event.hand != EquipmentSlot.HAND
                || event.useInteractedBlock() == Event.Result.DENY
            ) {
                return
            }

            event.setUseInteractedBlock(Event.Result.DENY)
            event.setUseItemInHand(Event.Result.DENY)

            guiBlock.open(event.player)
        }

        @EventHandler
        private fun onBreak(event: RebarBlockBreakEvent) {
            if (event.rebarBlock is GuiRebarBlock) {
                guiBlocks.remove(event.rebarBlock)!!.closeForAllViewers()
            }
        }

        @EventHandler
        private fun onUnload(event: RebarBlockUnloadEvent) {
            if (event.rebarBlock is GuiRebarBlock) {
                guiBlocks.remove(event.rebarBlock)!!.closeForAllViewers()
            }
        }
    }
}