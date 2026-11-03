package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.RebarBlockBreakEvent
import io.github.pylonmc.rebar.event.RebarBlockLoadEvent
import io.github.pylonmc.rebar.event.RebarBlockPlaceEvent
import io.github.pylonmc.rebar.event.RebarBlockUnloadEvent
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.dialog.DialogResponseView
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import java.util.*

/**
 * A simple interface that opens a confirmation dialog when the block is right clicked.
 *
 * To use this interface, simply override [ConfirmationDialogGuiRebarBlock.createDialogBase]
 * to return the dialog you want to be opened when the block is right clicked.
 *
 * See [Paper docs](https://docs.papermc.io/paper/dev/dialogs/) for information on how to make dialogs.
 *
 * API Note: the reason this onyl allows for confirmation dialogs is fundamentally the fact that the client does not tell
 * the server that a dialog has been closed, which we need to determine wether to close it when the block is broken.
 * Restricting the dialog type to only confirmation (which I expect is the majority type for blocks) allows us to inject
 * a handler into the action buttons, so that we can dtetct if the player has closed it or not.
 *
 * @see DialogRegistryEntry.Builder
 */
interface ConfirmationDialogGuiRebarBlock : NoVanillaInventoryRebarBlock {

    /**
     * Creates a dialog base for the dialog. This will be called every time someone opens the dialog.
     */
    fun createDialogBase(): DialogBase

    /**
     * Text for the "yes" button. By default the translation key "rebar.ok"
     */
    val yesButton: Component
        get() = Component.translatable("rebar.ok")

    /**
     * Text for the "no" button. By default the translation key "rebar.cancel"
     */
    val noButton: Component
        get() = Component.translatable("rebar.cancel")

    /**
     * Function to run on the "yes" action
     */
    fun onYesAction(response: DialogResponseView, audience: Audience) {}

    /**
     * Function to run on the "no" action
     */
    fun onNoAction(response: DialogResponseView, audience: Audience) {}

    fun open(player: Player) {
        val dialog = Dialog.create {
            it.empty()
                .base(createDialogBase())
                .type(
                    DialogType.confirmation(
                        ActionButton.builder(yesButton)
                            .action(
                                DialogAction.customClick(
                                    { response, audience ->
                                        onYesAction(response, audience)
                                        openDialogs[this]!!.remove(audience)
                                    },
                                    ClickCallback.Options.builder().build()
                                )
                            )
                            .build(),
                        ActionButton.builder(noButton)
                            .action(
                                DialogAction.customClick(
                                    { response, audience ->
                                        onNoAction(response, audience)
                                        openDialogs[this]!!.remove(audience)
                                    },
                                    ClickCallback.Options.builder().build()
                                )
                            )
                            .build()
                    )
                )
        }
        player.showDialog(dialog)
        openDialogs[this]!!.add(player)
    }

    companion object : Listener {
        private val openDialogs = IdentityHashMap<ConfirmationDialogGuiRebarBlock, MutableSet<Player>>()

        @EventHandler
        private fun onPlace(event: RebarBlockPlaceEvent) {
            if (event.rebarBlock is ConfirmationDialogGuiRebarBlock) {
                openDialogs[event.rebarBlock] = Collections.newSetFromMap(WeakHashMap())
            }
        }

        @EventHandler
        private fun onLoad(event: RebarBlockLoadEvent) {
            if (event.rebarBlock is ConfirmationDialogGuiRebarBlock) {
                openDialogs[event.rebarBlock] = Collections.newSetFromMap(WeakHashMap())
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        private fun onInteract(event: PlayerInteractEvent) {
            val guiBlock = BlockStorage.getAs(ConfirmationDialogGuiRebarBlock::class.java, event.clickedBlock ?: return) ?: return

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
            if (event.rebarBlock is ConfirmationDialogGuiRebarBlock) {
                for (player in openDialogs.remove(event.rebarBlock)!!) {
                    player.closeDialog()
                }
            }
        }

        @EventHandler
        private fun onUnload(event: RebarBlockUnloadEvent) {
            if (event.rebarBlock is ConfirmationDialogGuiRebarBlock) {
                for (player in openDialogs.remove(event.rebarBlock)!!) {
                    player.closeDialog()
                }
            }
        }
    }
}