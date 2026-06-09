package io.github.pylonmc.rebar.recipe.slot.item

import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.block.interfaces.NoVanillaInventoryRebarBlock
import io.github.pylonmc.rebar.logistics.LogisticGroupType
import io.github.pylonmc.rebar.recipe.slot.item.implementation.BrewingStandFuelItemSlot
import io.github.pylonmc.rebar.recipe.slot.item.implementation.BrewingStandPotionItemSlot
import io.github.pylonmc.rebar.recipe.slot.item.implementation.ChiseledBookshelfFuelItemSlot
import io.github.pylonmc.rebar.recipe.slot.item.implementation.CrafterItemSlot
import io.github.pylonmc.rebar.recipe.slot.item.implementation.FurnaceFuelItemSlot
import io.github.pylonmc.rebar.recipe.slot.item.implementation.JukeboxItemSlot
import io.github.pylonmc.rebar.recipe.slot.item.implementation.VanillaInventoryItemSlot
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import org.bukkit.block.*

/**
 * A collection of item slots that share the same functionality (i.e. an inventory).
 *
 * For example, a machine might have an 'input' group with 9 slots, a
 * 'catalyst' group with 1 slot, and a 'output' group with 9 slots.
 */
class ItemSlotGroup(
    val slotType: LogisticGroupType,
    val slots: MutableList<ItemSlot>
) {

    constructor(slotType: LogisticGroupType, vararg slots: ItemSlot) : this(slotType, slots.toMutableList())

    /**
     * Returns whether the provided item stack can be inserted into any slots
     * within the group.
     *
     * This can be used to only allow certain items to be inserted into this
     * slot (or to prevent certain items from being inserted).
     *
     * Any logic in this function should disregard the stack amount; this is
     * checked separately.
     */
    var filter: ((ItemStack) -> Boolean)? = null

    fun withFilter(filter: (ItemStack) -> Boolean) = apply {
        this.filter = filter
    }

    companion object {

        @JvmStatic
        fun getVanillaLogisticSlots(block: Block?): Map<String, ItemSlotGroup> {
            if (block == null || BlockStorage.get(block) is NoVanillaInventoryRebarBlock) {
                return mapOf()
            }

            return when (val blockState = block.getState(false)) {
                is Furnace -> mapOf(
                    "input" to ItemSlotGroup(
                        LogisticGroupType.INPUT,
                        VanillaInventoryItemSlot(block, blockState.inventory, 0)
                    ),
                    "fuel" to ItemSlotGroup(
                        LogisticGroupType.INPUT,
                        FurnaceFuelItemSlot(block, blockState.inventory, 1)
                    ),
                    "output" to ItemSlotGroup(
                        LogisticGroupType.OUTPUT,
                        VanillaInventoryItemSlot(block, blockState.inventory, 2)
                    ),
                )
                is BrewingStand -> mapOf(
                    "output" to ItemSlotGroup(
                        LogisticGroupType.BOTH,
                        BrewingStandPotionItemSlot(block, blockState.inventory, 0),
                        BrewingStandPotionItemSlot(block, blockState.inventory, 1),
                        BrewingStandPotionItemSlot(block, blockState.inventory, 2),
                    ),
                    "input" to ItemSlotGroup(
                        LogisticGroupType.INPUT,
                        VanillaInventoryItemSlot(block, blockState.inventory, 3)
                    ),
                    "fuel" to ItemSlotGroup(
                        LogisticGroupType.INPUT,
                        BrewingStandFuelItemSlot(block, blockState.inventory, 4)
                    ),
                )
                is ChiseledBookshelf -> {
                    val slots = mutableListOf<ItemSlot>()
                    for (slot in 0..<blockState.inventory.size) {
                        slots.add(ChiseledBookshelfFuelItemSlot(block, blockState.inventory, slot))
                    }
                    mapOf("inventory" to ItemSlotGroup(LogisticGroupType.BOTH, slots))
                }
                is Jukebox -> mapOf(
                    "inventory" to ItemSlotGroup(
                        LogisticGroupType.BOTH,
                        JukeboxItemSlot(block, blockState.inventory, 0)
                    ),
                )
                is Dispenser, is Dropper, is Hopper, is Barrel, is DoubleChest, is Chest, is Shelf, is ShulkerBox -> {
                    val slots = mutableListOf<ItemSlot>()
                    for (slot in 0..<blockState.inventory.size) {
                        slots.add(VanillaInventoryItemSlot(block, blockState.inventory, slot))
                    }
                    mapOf("inventory" to ItemSlotGroup(LogisticGroupType.BOTH, slots))
                }
                is Crafter -> {
                    val slots = mutableListOf<ItemSlot>()
                    for (slot in 0..<blockState.inventory.size) {
                        slots.add(CrafterItemSlot(block, blockState.inventory, slot))
                    }
                    mapOf("inventory" to ItemSlotGroup(LogisticGroupType.INPUT, slots))
                }
                else -> mapOf()
            }
        }
    }
}