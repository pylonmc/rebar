package io.github.pylonmc.rebar.nms.recipe.util

import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.nms.recipe.util.StackedItemContentsWrapper.rawGetter
import io.papermc.paper.inventory.recipe.ItemOrExact
import net.minecraft.world.entity.player.StackedContents
import net.minecraft.world.entity.player.StackedItemContents
import net.minecraft.world.item.ItemStack
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import kotlin.math.min

object StackedItemContentsWrapper {
    var initialized = false
    lateinit var rawGetter: MethodHandle

    fun initialize() {
        if (initialized) return
        val lookup = MethodHandles.privateLookupIn(StackedItemContents::class.java, MethodHandles.lookup())
        rawGetter = lookup.findGetter(StackedItemContents::class.java, "raw", StackedContents::class.java)

        initialized = true
    }
}

@Suppress("UNCHECKED_CAST")
fun StackedItemContents.getRaw(): StackedContents<ItemOrExact> = rawGetter.invokeExact(this) as StackedContents<ItemOrExact>

/**
 *
 * Behaves like the StackedItemContents#accountStack, however for Rebar items we instead
 * account them as ItemOrExact#Exact when added to the StackedContents that handles crafting,
 * so that only exact picks, and not material pick, will show up as valid
 *
 * @param stack stack to add
 * @param maxStackSize max stack size of the itemstack, by default obtained with DataComponents#MAX_STACK_SIZE
 */
fun StackedItemContents.accountStackRebar(stack: ItemStack, maxStackSize: Int = stack.maxStackSize) {
    if (stack.isEmpty) return

    // Determine if this is a Rebar item
    if (RebarItem.isRebarItem(stack.bukkitStack)) {
        val min = min(maxStackSize, stack.count)
        val r = ItemOrExact.Exact(stack.copy())
        this.getRaw().account(r, min)
        return
    }

    this.accountSimpleStack(stack)
}