package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.event.RebarBlockBreakEvent
import io.github.pylonmc.rebar.event.RebarBlockUnloadEvent
import io.github.pylonmc.rebar.logistics.LogisticGroup
import io.github.pylonmc.rebar.logistics.LogisticGroupType
import io.github.pylonmc.rebar.logistics.slot.LogisticSlot
import io.github.pylonmc.rebar.logistics.slot.VirtualInventoryLogisticSlot
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.jetbrains.annotations.ApiStatus
import xyz.xenondevs.invui.inventory.VirtualInventory
import java.util.IdentityHashMap
import kotlin.collections.contains
import kotlin.collections.getOrPut

/**
 * A block which can have items removed or added via a logistics system.
 *
 * Item addition/removal is managed by 'groups' of 'logistic slots'. Each
 * group has a unique name and is either an input or output group.
 *
 * Slots can be in multiple groups. For example, you could have a 'buffer'
 * slot be in both an input and an output group, allowing items to be both
 * inserted and removed.
 *
 * To use this interface, all you need to do is call `createLogisticGroup`
 * to create all the logistic groups you want in `postInitialise`.
 */
interface LogisticRebarBlock {

    /**
     * Automatically implemented when this interface is implemented by a [io.github.pylonmc.rebar.block.RebarBlock]
     */
    val block: Block

    fun createLogisticGroup(groupName: String, group: LogisticGroup) {
        val logisticBlockData = (logisticBlocks.getOrPut(this) { mutableMapOf() })
        check(!logisticBlockData.contains(groupName)) { "The slot group $groupName already exists" }
        logisticBlockData.put(groupName, group)
    }

    fun createLogisticGroup(groupName: String, slotType: LogisticGroupType, vararg slots: LogisticSlot)
        = createLogisticGroup(groupName, LogisticGroup(slotType, *slots))

    fun createLogisticGroup(groupName: String, slotType: LogisticGroupType, slots: List<LogisticSlot>)
        = createLogisticGroup(groupName, LogisticGroup(slotType, *slots.toTypedArray()))

    fun createLogisticGroup(groupName: String, slotType: LogisticGroupType, inventory: VirtualInventory) {
        val slots = mutableListOf<LogisticSlot>()
        for (slot in 0..<inventory.size) {
            slots.add(VirtualInventoryLogisticSlot(inventory, slot))
        }
        createLogisticGroup(groupName, slotType, slots)
    }

    fun getLogisticGroup(groupName: String): LogisticGroup?
        = getLogisticGroups()[groupName]

    fun getLogisticGroupOrThrow(groupName: String): LogisticGroup
        = getLogisticGroup(groupName) ?: error("Group $groupName does not exist")

    fun getLogisticGroups(): Map<String, LogisticGroup>
        = logisticBlocks.getOrPut(this) { mutableMapOf() }

    @ApiStatus.Internal
    companion object : Listener {

        private val logisticBlocks = IdentityHashMap<LogisticRebarBlock, MutableMap<String, LogisticGroup>>()

        @EventHandler
        private fun onBreak(event: RebarBlockBreakEvent) {
            val block = event.rebarBlock
            if (block is LogisticRebarBlock) {
                logisticBlocks.remove(block)
            }
        }

        @EventHandler
        private fun onUnload(event: RebarBlockUnloadEvent) {
            val block = event.rebarBlock
            if (block is LogisticRebarBlock) {
                logisticBlocks.remove(block)
            }
        }
    }
}