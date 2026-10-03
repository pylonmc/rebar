package io.github.pylonmc.rebar.nms.recipe

import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.nms.recipe.util.StackedItemContentsWrapper
import io.github.pylonmc.rebar.nms.recipe.util.accountStackRebar
import io.papermc.paper.inventory.recipe.ItemOrExact
import net.minecraft.recipebook.PlaceRecipeHelper
import net.minecraft.recipebook.ServerPlaceRecipe
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.StackedItemContents
import net.minecraft.world.inventory.AbstractCraftingMenu
import net.minecraft.world.inventory.RecipeBookMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingRecipe
import net.minecraft.world.item.crafting.RecipeHolder
import org.bukkit.craftbukkit.inventory.CraftItemStack
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import kotlin.math.min

/**
 * Handling Rebar item placing and accounting, delegates most of the stuff to
 * nms's ServerPlaceRecipe class, tries to match the method signatures and implements
 * ServerPlaceRecipe#CraftingMenuAccess as we don't need an anonymous object as we are
 * only going to change that specific case
 */
class RebarServerPlaceRecipe private constructor(
    private val menu: AbstractCraftingMenu,
    private val player: ServerPlayer,
    private val inputGridSlots: MutableList<Slot>
) : ServerPlaceRecipe.CraftingMenuAccess<CraftingRecipe> {
    private lateinit var delegate: ServerPlaceRecipe<*>

    init {
        StackedItemContentsWrapper.initialize()
        initialize()
    }

    override fun clearCraftingContent() {
        menu.resultSlots.clearContent()
        menu.craftSlots.clearContent()
    }

    override fun recipeMatches(recipe1: RecipeHolder<CraftingRecipe>): Boolean {
        return recipe1.value().matches(
            menu.craftSlots.asCraftInput(),
            player.level()
        )
    }

    override fun fillCraftSlotsStackedContents(stackedItemContents: StackedItemContents) {
        for (stack in menu.craftSlots.contents) {
            stackedItemContents.accountStackRebar(stack)
        }
    }

    /**
     * We override this because we need to use our own placeRecipe
     */
    private fun tryPlaceRecipe(recipe: RecipeHolder<CraftingRecipe>, stackedItemContents: StackedItemContents): RecipeBookMenu.PostPlaceAction {
        if (stackedItemContents.canCraft(recipe.value(), null)) {
            this.placeRecipe(recipe, stackedItemContents)
            this.player.inventory.setChanged()
            return RecipeBookMenu.PostPlaceAction.NOTHING
        } else {
            this.clearGrid()
            this.player.inventory.setChanged()
            return RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE
        }
    }

    private fun clearGrid() {
        methodMap["clearGrid"]!!.invokeExact(delegate)
    }

    /**
     * We override this because we need to use our own moveItemToGrid
     */
    private fun placeRecipe(recipe: RecipeHolder<CraftingRecipe>, stackedItemContents: StackedItemContents) {
        val flag = this.recipeMatches(recipe)
        val selectedRecipe = recipe.value()
        val biggestCraftableStack = stackedItemContents.getBiggestCraftableStack(selectedRecipe, null)
        if (flag) {
            for (slot in this.inputGridSlots) {
                val item = slot.item
                if (!item.isEmpty && min(biggestCraftableStack, item.maxStackSize) < item.count + 1) {
                    return
                }
            }
        }

        val i = this.calculateAmountToCraft(biggestCraftableStack, flag)
        val list = ArrayList<ItemOrExact>()
        if (!stackedItemContents.canCraft(selectedRecipe, i) { e: ItemOrExact? -> list.add(e!!) }) return

        val i1: Int = clampToMaxStackSize(i, list)
        if (i1 != i) {
            list.clear()
            if (!stackedItemContents.canCraft(
                    selectedRecipe,
                    i1
                ) { e: ItemOrExact? -> list.add(e!!) }
            ) {
                return
            }
        }

        this.clearGrid()
        PlaceRecipeHelper.placeRecipe(
            this.menu.gridWidth,
            this.menu.gridHeight,
            selectedRecipe,
            selectedRecipe.placementInfo().slotsToIngredientIndex()
        ) { item1: Int?, slot1: Int, _: Int, _: Int ->
            if (item1 != -1) {
                val slot2 = this.inputGridSlots[slot1]
                val holder = list[item1!!]
                var i2 = i1

                while (i2 > 0) {
                    i2 = this.moveItemToGrid(slot2, holder, i2)
                    if (i2 == -1) {
                        return@placeRecipe
                    }
                }
            }
        }
    }

    private fun calculateAmountToCraft(max: Int, recipeMatches: Boolean): Int {
        return methodMap["calculateAmountToCraft"]!!.invokeExact(delegate, max, recipeMatches) as Int
    }

    /**
     * We override this because we need to use our own findSlotMatchingCraftingIngredient
     */
    private fun moveItemToGrid(slot: Slot, item: ItemOrExact, count: Int): Int {
        val item1 = slot.item
        val matchingSlot = findSlotMatchingCraftingIngredient(this.player.inventory.contents, item, item1)
        if (matchingSlot == -1) {
            return -1
        }

        val item2 = this.player.inventory.getItem(matchingSlot)
        val itemStack = if (count < item2.count) {
            this.player.inventory.removeItem(matchingSlot, count)
        } else {
            this.player.inventory.removeItemNoUpdate(matchingSlot)
        }

        val count1 = itemStack.count
        if (item1.isEmpty) {
            slot.set(itemStack)
        } else {
            item1.grow(count1)
        }

        return count - count1
    }

    private fun testClearGrid(): Boolean {
        return methodMap["testClearGrid"]!!.invokeExact(delegate) as Boolean
    }

    /**
     * Behave like normal, however skip Rebar items from material matches
     */
    fun findSlotMatchingCraftingIngredient(items: List<ItemStack>, item: ItemOrExact, stack: ItemStack): Int {
        for (i in items.indices) {
            val itemStack = items[i]
            if (itemStack.isEmpty) continue

            if (!item.matches(itemStack)) continue

            if (item is ItemOrExact.Item) {
                if (!Inventory.isUsableForCrafting(itemStack)) continue
                if (RebarItem.isRebarItem(CraftItemStack.asBukkitMirror(itemStack))) continue // skip our Rebar items
            }

            if (stack.isEmpty || ItemStack.isSameItemSameComponents(stack, itemStack)) {
                return i
            }
        }

        return -1
    }

    companion object {
        var initialized = false
        val methodMap = HashMap<String, MethodHandle>()
        lateinit var constructor: MethodHandle

        fun initialize() {
            if (initialized) return
            initialized = true

            val lookup = MethodHandles.privateLookupIn(ServerPlaceRecipe::class.java, MethodHandles.lookup())

            for (method in ServerPlaceRecipe::class.java.declaredMethods) {
                methodMap[method.name] = lookup.unreflect(method)
            }

            constructor = lookup.findConstructor(
                ServerPlaceRecipe::class.java,
                MethodType.methodType(
                    Void.TYPE,
                    ServerPlaceRecipe.CraftingMenuAccess::class.java,
                    Inventory::class.java,
                    Boolean::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    List::class.java,
                    List::class.java
                )
            )
        }

        fun placeRecipe(
            menu: AbstractCraftingMenu,
            player: ServerPlayer,
            inputGridSlots: MutableList<Slot>,
            slotsToClear: MutableList<Slot>,
            recipe: RecipeHolder<CraftingRecipe>,
            useMaxItems: Boolean
        ): RecipeBookMenu.PostPlaceAction {
            val serverPlaceRecipe = RebarServerPlaceRecipe(
                menu,
                player,
                inputGridSlots
            )

            @Suppress("UNCHECKED_CAST")
            serverPlaceRecipe.delegate = constructor.invokeExact(
                serverPlaceRecipe as ServerPlaceRecipe.CraftingMenuAccess<CraftingRecipe>, // important for invokeExact
                player.inventory,
                useMaxItems,
                menu.gridWidth, menu.gridHeight,
                inputGridSlots,
                slotsToClear
            ) as ServerPlaceRecipe<CraftingRecipe>

            if (!player.isCreative && !serverPlaceRecipe.testClearGrid()) {
                return RecipeBookMenu.PostPlaceAction.NOTHING
            }

            val stackedItemContents = StackedItemContents()
            stackedItemContents.initializeExtras(recipe.value(), null)

            for (itemStack in player.inventory) {
                stackedItemContents.accountStackRebar(itemStack)
            }

            serverPlaceRecipe.fillCraftSlotsStackedContents(stackedItemContents)
            return serverPlaceRecipe.tryPlaceRecipe(recipe, stackedItemContents)
        }

        private fun clampToMaxStackSize(amount: Int, items: MutableList<ItemOrExact>): Int {
            var amount = amount
            for (holder in items) {
                amount = min(amount, holder.maxStackSize)
            }

            return amount
        }
    }
}
