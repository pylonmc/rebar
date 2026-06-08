package io.github.pylonmc.rebar.nms.inventory

import net.minecraft.resources.Identifier
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerListener
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemStack
import net.minecraft.world.item.ItemStack as NmsItemStack

/**
 * A [ContainerListener] whose equality is defined purely by its [key],
 * ensuring no duplicates when using [AbstractContainerMenu.addSlotListener]
 * multiple times
 */
class KeyedContainerListener(
    private val key: Identifier,
    private val listener: (inventoryView: InventoryView, slot: Int, oldItemStack: ItemStack?, newItemStack: ItemStack?) -> Unit
) : ContainerListener {

    override fun slotChanged(container: AbstractContainerMenu, slot: Int, oldItemStack: NmsItemStack, newItemStack: NmsItemStack) {
        listener(container.bukkitView, slot, oldItemStack.bukkitStack, newItemStack.bukkitStack)
    }

    override fun slotChanged(container: AbstractContainerMenu, slot: Int, itemStack: NmsItemStack) {}
    override fun dataChanged(container: AbstractContainerMenu, id: Int, value: Int) {}

    override fun equals(other: Any?): Boolean {
        return other is KeyedContainerListener && other.key == key
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }

    override fun toString(): String {
        return "KeyedContainerListener(key=$key)"
    }

}