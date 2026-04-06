package io.github.pylonmc.rebar.logistics

import io.github.pylonmc.rebar.logistics.slot.*
import org.bukkit.block.*
import org.bukkit.inventory.ItemStack

/**
 * A collection of logistic slots that share the same functionality.
 *
 * For example, a machine might have an 'input' group with 9 slots, a
 * 'catalyst' group with 1 slot, and a 'output' group with 9 slots.
 */
class LogisticGroup(
    val slotType: LogisticGroupType,
    val slots: List<LogisticSlot>
) {

    constructor(slotType: LogisticGroupType, vararg slots: LogisticSlot) : this(slotType, slots.toList())

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
        fun getVanillaLogisticSlots(block: Block?): Map<String, LogisticGroup> {
            return when (val blockData = block?.state) {
                is Furnace -> mapOf<String, LogisticGroup>(
                    "input" to LogisticGroup(LogisticGroupType.INPUT, VanillaInventoryLogisticSlot(blockData.inventory, 0)),
                    "fuel" to LogisticGroup(LogisticGroupType.INPUT, FurnaceFuelLogisticSlot(blockData.inventory, 1)),
                    "output" to LogisticGroup(LogisticGroupType.OUTPUT, VanillaInventoryLogisticSlot(blockData.inventory, 2)),
                )
                is BrewingStand -> mapOf<String, LogisticGroup>(
                    "output" to LogisticGroup(LogisticGroupType.BOTH,
                        BrewingStandPotionLogisticSlot(blockData.inventory, 0),
                        BrewingStandPotionLogisticSlot(blockData.inventory, 1),
                        BrewingStandPotionLogisticSlot(blockData.inventory, 2),
                    ),
                    "input" to LogisticGroup(LogisticGroupType.INPUT, VanillaInventoryLogisticSlot(blockData.inventory, 3)),
                    "fuel" to LogisticGroup(LogisticGroupType.INPUT, BrewingStandFuelLogisticSlot(blockData.inventory, 4)),
                )
                is ChiseledBookshelf -> {
                    val slots = mutableListOf<LogisticSlot>()
                    for (slot in 0..<blockData.inventory.size) {
                        slots.add(ChiseledBookshelfFuelLogisticSlot(blockData.inventory, slot))
                    }
                    return mapOf<String, LogisticGroup>("inventory" to LogisticGroup(LogisticGroupType.BOTH, slots))
                }
                is Jukebox -> mapOf<String, LogisticGroup>(
                    "inventory" to LogisticGroup(LogisticGroupType.BOTH, JukeboxLogisticSlot(blockData.inventory, 0)),
                )
                is Dispenser, is Dropper, is Hopper, is Barrel, is DoubleChest, is Chest, is Shelf, is ShulkerBox -> {
                    val slots = mutableListOf<LogisticSlot>()
                    for (slot in 0..<blockData.inventory.size) {
                        slots.add(VanillaInventoryLogisticSlot(blockData.inventory, slot))
                    }
                    return mapOf<String, LogisticGroup>("inventory" to LogisticGroup(LogisticGroupType.BOTH, slots))
                }
                is Crafter -> {
                    val slots = mutableListOf<LogisticSlot>()
                    for (slot in 0..<blockData.inventory.size) {
                        slots.add(CrafterLogisticSlot(block, blockData.inventory, slot))
                    }
                    return mapOf<String, LogisticGroup>("inventory" to LogisticGroup(LogisticGroupType.INPUT, slots))
                }
                else -> mapOf<String, LogisticGroup>()
            }
        }
    }
}