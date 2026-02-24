package io.github.pylonmc.rebar.recipe

import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.base.*
import io.github.pylonmc.rebar.item.research.Research.Companion.canCraft
import io.github.pylonmc.rebar.recipe.vanilla.CookingRecipeWrapper
import io.github.pylonmc.rebar.recipe.vanilla.ShapedRecipeType
import io.github.pylonmc.rebar.recipe.vanilla.VanillaRecipeType
import io.github.pylonmc.rebar.util.isRebarAndIsNot
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.event.player.CartographyItemEvent
import org.bukkit.Keyed
import org.bukkit.block.Crafter
import org.bukkit.block.Furnace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockCookEvent
import org.bukkit.event.block.CrafterCraftEvent
import org.bukkit.event.inventory.*
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.inventory.StonecutterInventory
import org.bukkit.inventory.meta.Damageable

internal object RebarRecipeListener : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onPreCraft(e: PrepareItemCraftEvent) {
        val recipe = e.recipe
        // All recipe types but MerchantRecipe implement Keyed
        if (recipe !is Keyed) return
        val inventory = e.inventory

        val hasRebarItems = inventory.any { it.isRebarAndIsNot<VanillaCraftingItem>() }
        val isNotRebarCraftingRecipe = recipe.key in VanillaRecipeType.nonRebarRecipes

        // Prevent the erroneous crafting of vanilla items with Rebar ingredients
        if (hasRebarItems && isNotRebarCraftingRecipe) {
            inventory.result = null
        }

        // Allow merging Rebar tools/weapons/armour in crafting grid unless marked with RebarUnmergeable
        if (hasRebarItems && e.isRepair) {
            var firstItem: ItemStack? = null
            var secondItem: ItemStack? = null
            for (item in e.inventory.matrix) {
                if (item != null && !item.isEmpty)  {
                    if (firstItem == null) {
                        firstItem = item
                    } else if (secondItem == null) {
                        secondItem = item
                    } else {
                        error("How the hell is it possible that there are more than two items in an item repair recipe")
                    }
                }
            }
            check(firstItem != null)
            check(secondItem != null)
            if (firstItem.isSimilar(secondItem)) {
                val rebarItem = RebarItem.fromStack(firstItem)!!
                if (rebarItem !is RebarUnmergeable) {
                    val result = rebarItem.schema.getItemStack()
                    val resultDamage = inventory.result!!.getData(DataComponentTypes.DAMAGE)!!
                    result.setData(DataComponentTypes.DAMAGE, resultDamage)
                    inventory.result = result
                }
            } else {
                inventory.result = null
            }
        }

        // Prevent crafting of unresearched items
        val rebarItemResult = RebarItem.fromStack(recipe.result)
        val anyViewerDoesNotHaveResearch = rebarItemResult != null && e.viewers.none {
            it is Player && it.canCraft(rebarItemResult, true)
        }
        if (anyViewerDoesNotHaveResearch) {
            inventory.result = null
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onCartography(e: CartographyItemEvent) {
        val inventory = e.inventory
        val hasRebarItems = inventory.any { it.isRebarAndIsNot<VanillaCraftingItem>() }

        if (hasRebarItems) {
            inventory.result = null
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onCrafterCraft(e: CrafterCraftEvent) {
        val crafterState = e.block.state as? Crafter ?: return
        val inventory = crafterState.inventory

        val hasRebarItems = inventory.any { it.isRebarAndIsNot<VanillaCraftingItem>() }
        if (!hasRebarItems) {
            return
        }

        val crafter = e.block.state as Crafter

        // TODO make this not horrible (both for performance and readability) - see https://github.com/pylonmc/rebar/issues/545
        for (recipe in ShapedRecipeType.recipes) {
            val craftingRecipe = recipe.craftingRecipe
            if (craftingRecipe is ShapedRecipe) {
                var i = 0
                var isValid = true
                recipeLoop@ for (row in craftingRecipe.shape) {
                    for (index in row) {
                        val ingredient = craftingRecipe.choiceMap[index]
                        if (ingredient != null) {
                            val actual = crafter.inventory.getItem(i)
                            if (actual == null || !ingredient.test(actual)) {
                                isValid = false
                                break@recipeLoop
                            }
                        }
                        i++
                    }
                }
                if (isValid) {
                    e.result = craftingRecipe.result
                    return
                }

            } else if (craftingRecipe is ShapelessRecipe) {
                val usedSlots = mutableSetOf<Int>()
                for (ingredient in craftingRecipe.choiceList) {
                    var isValid = false
                    for (crafterIndex in 0..<crafter.inventory.size) {
                        val actual = crafter.inventory.getItem(crafterIndex)
                        if (crafterIndex in usedSlots || actual == null || !ingredient.test(actual)) {
                            continue
                        }
                        isValid = true
                        usedSlots.add(crafterIndex)
                    }
                    if (isValid) {
                        e.result = craftingRecipe.result
                        return
                    }
                }

            } else {
                e.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun itemInsertEvent(e: InventoryClickEvent) {
        val inventory = e.inventory;
        if (inventory is StonecutterInventory) {
            val input = inventory.inputItem ?: return

            if (input.isRebarAndIsNot<VanillaCraftingItem>()) {
                e.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onCook(e: BlockCookEvent) {
        if (RebarItem.fromStack(e.source) == null) return

        var rebarRecipe: CookingRecipeWrapper? = null
        for (recipe in RecipeType.vanillaCookingRecipes()) {
            if (recipe.key !in VanillaRecipeType.nonRebarRecipes && recipe.recipe.inputChoice.test(e.source)) {
                e.result = recipe.recipe.result.clone()
                rebarRecipe = recipe
                break
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onFuelBurn(e: FurnaceBurnEvent) {
        if (e.fuel.isRebarAndIsNot<VanillaCookingFuel>()) {
            e.isCancelled = true
            return
        }

        val furnace = (e.block.state as Furnace)
        val input = furnace.inventory.smelting
        if (input != null && input.isRebarAndIsNot<VanillaCookingItem>()) {
            var rebarRecipe: CookingRecipeWrapper? = null
            for (recipe in RecipeType.vanillaCookingRecipes()) {
                if (recipe.key !in VanillaRecipeType.nonRebarRecipes && recipe.recipe.inputChoice.test(input)) {
                    rebarRecipe = recipe
                    break
                }
            }
            val isFurnaceOutputValidToPutRecipeResultIn = rebarRecipe != null
                    && (furnace.inventory.result == null || rebarRecipe.isOutput(furnace.inventory.result!!))
            if (rebarRecipe == null || !isFurnaceOutputValidToPutRecipeResultIn) {
                e.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onSmith(e: PrepareSmithingEvent) {
        val inv = e.inventory
        val recipe = inv.recipe
        if (recipe !is Keyed) return

        // Prevent the erroneous smithing of vanilla items with Rebar ingredients
        val hasRebarItem = inv.inputMineral.isRebarAndIsNot<VanillaSmithingMineral>()
                || inv.inputTemplate.isRebarAndIsNot<VanillaSmithingTemplate>()
        if (hasRebarItem && recipe.key in VanillaRecipeType.nonRebarRecipes) {
            e.result = null
            return
        }

        // Prevent crafting of unresearched items
        val rebarItemResult = RebarItem.fromStack(recipe.result)
        val anyViewerDoesNotHaveResearch = rebarItemResult != null && e.viewers.none {
            it is Player && it.canCraft(rebarItemResult, true)
        }
        if (anyViewerDoesNotHaveResearch) {
            inv.result = null
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onAnvilSlotChanged(e: PrepareAnvilEvent) {
        val inventory = e.inventory
        val firstItem = inventory.firstItem
        val secondItem = inventory.secondItem
        // Check if the result is a repaired item and if so, cancel it.
        if(firstItem != null && e.result != null
            && firstItem.itemMeta is Damageable && e.result!!.itemMeta is Damageable
            && (firstItem.itemMeta as Damageable).damage < (e.result!!.itemMeta as Damageable).damage){
            e.result = null
            return
        }
        // Check if either input is a rebar item without VanillaAnvilItem and if the output is vanilla, cancel it
        if((firstItem.isRebarAndIsNot<VanillaAnvilItem>() || secondItem.isRebarAndIsNot<VanillaAnvilItem>()) && RebarItem.fromStack(e.result) == null) {
            e.result = null
            return
        }
    }
}
