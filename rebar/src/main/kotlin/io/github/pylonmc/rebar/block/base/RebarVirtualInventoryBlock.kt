package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.context.BlockBreakContext
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.event.RebarBlockDeserializeEvent
import io.github.pylonmc.rebar.event.RebarBlockSerializeEvent
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.MustBeInvokedByOverriders
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.invui.inventory.event.UpdateReason

/**
 * Saves and loads virtual inventories associated with the block.
 *
 * When the block is broken, the contents of the virtual inventories will be dropped.
 *
 * See [InvUI docs](https://docs.xenondevs.xyz/invui/) for more information on virtual inventories.
 *
 * @see Gui
 * @see VirtualInventory
 * @see RebarGuiBlock
 */
interface RebarVirtualInventoryBlock : RebarBreakHandler {

    /**
     * A map of inventory names to virtual inventories associated with this block
     */
    fun getVirtualInventories(): Map<String, VirtualInventory>

    @MustBeInvokedByOverriders
    override fun onBreak(drops: MutableList<ItemStack>, context: BlockBreakContext) {
        for (inventory in getVirtualInventories().values) {
            for (item in inventory.items) {
                item?.let(drops::add)
            }
        }
    }

    @ApiStatus.Internal
    companion object : Listener {

        private val virtualInventoryItemsKey = rebarKey("virtual_inventory_items")
        private val virtualInventoryItemsType = RebarSerializers.MAP.mapTypeFrom(
            RebarSerializers.STRING,
            RebarSerializers.LIST.listTypeFrom(RebarSerializers.ITEM_STACK)
        )

        @EventHandler
        private fun onDeserialize(event: RebarBlockDeserializeEvent) {
            val block = event.rebarBlock
            if (block !is RebarVirtualInventoryBlock) return
            val virtualInventoryItems = event.pdc.getOrDefault(virtualInventoryItemsKey, virtualInventoryItemsType, emptyMap())
            val inventories = block.getVirtualInventories()

            // Copy stored items to inventory - have to manually set each one
            for ((name, items) in virtualInventoryItems) {
                val inventory = inventories[name] ?: continue
                for ((index, item) in items.withIndex()) {
                    // Suppress any events when we set the item
                    // Reasoning: Items may set update handlers in the constructor, but then having those
                    // immediately called by deserialization logic (often before the rest of the machine's
                    // data has finished loading) is unexpected behaviour
                    inventory.forceSetItem(UpdateReason.SUPPRESSED, index, item.takeUnless { it.isEmpty })
                }
            }
        }

        @EventHandler
        private fun onSerialize(event: RebarBlockSerializeEvent) {
            val block = event.rebarBlock
            if (block !is RebarVirtualInventoryBlock) return
            event.pdc.set(
                virtualInventoryItemsKey,
                virtualInventoryItemsType,
                block.getVirtualInventories().mapValues { (_, inv) ->
                    inv.unsafeItems.map { it ?: ItemStack.empty() }
                }
            )
        }
    }
}