package io.github.pylonmc.rebar.logistics.slot

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.invui.inventory.event.UpdateReason

open class VirtualInventoryLogisticSlot(
    val inventory: VirtualInventory,
    val slot: Int
) : LogisticSlot {

    override fun getItemStack() = inventory.getUnsafeItem(slot)

    override fun getAmount() = getItemStack()?.amount?.toLong() ?: 0L

    override fun getMaxAmount(stack: ItemStack) = stack.maxStackSize.toLong()

    override fun set(stack: ItemStack?, amount: Long) {
        inventory.setItem(LogisticUpdateReason, slot, stack?.apply {
            this.amount = amount.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
        })
    }

    override fun canSet(stack: ItemStack?, amount: Long): Boolean {
        return !inventory.callPreUpdateEvent(
            LogisticUpdateReason,
            slot,
            inventory.getItem(slot),
            stack
        ).isCancelled
    }

    object LogisticUpdateReason : UpdateReason
}