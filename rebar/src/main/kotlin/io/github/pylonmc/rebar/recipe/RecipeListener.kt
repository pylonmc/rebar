package io.github.pylonmc.rebar.recipe

import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.base.*
import io.github.pylonmc.rebar.item.research.Research.Companion.canCraft
import io.github.pylonmc.rebar.recipe.vanilla.CraftingRecipeWrapper
import io.github.pylonmc.rebar.recipe.vanilla.VanillaRecipeType
import io.github.pylonmc.rebar.util.hashIgnoreAmount
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
import org.bukkit.event.block.CampfireStartEvent
import org.bukkit.event.block.CrafterCraftEvent
import org.bukkit.event.inventory.*
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.StonecutterInventory

internal object RebarRecipeListener : Listener {

    @Suppress("UnstableApiUsage")
    @EventHandler(priority = EventPriority.LOWEST)
    private fun onPreCraft(e: PrepareItemCraftEvent) {
        val recipe = e.recipe
        // All recipe types but MerchantRecipe implement Keyed
        if (recipe !is Keyed) return
        val inventory = e.inventory

        val hasRebarItems = inventory.any { it.isRebarAndIsNot<VanillaCraftingItem>() }

        // If vanilla ingredients matched a vanilla recipe, we leave it
        if (recipe.key in VanillaRecipeType.nonRebarRecipes && !hasRebarItems) {
            return
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
                    return
                }
            } else {
                inventory.result = null
            }
        }
        // Due to rebar ingredients possibly needing to ignore components (and thus using MaterialChoice)
        // we can't fully trust that the recipe returned by MC is correct
        var rebarRecipe: CraftingRecipeWrapper? = RebarRecipe.searchRecipes(
            RecipeType.VANILLA_SHAPED,
            recipe.key,
            RebarRecipe.hashShaped(e.inventory.matrix.toList())
        ) { it.matches(e.inventory.matrix.toList()) }
        if (rebarRecipe == null) {
            // Try shapeless instead
            rebarRecipe = RebarRecipe.searchRecipes(
                RecipeType.VANILLA_SHAPELESS,
                recipe.key,
                RebarRecipe.hashShapeless(e.inventory.matrix.toList())
            ) { it.matches(e.inventory.matrix.toList()) }
        }
        if (rebarRecipe == null) {
            inventory.result = null
            return
        }
        // Prevent crafting of unresearched items
        val rebarItemResult = RebarItem.fromStack(recipe.result)
        val anyViewerDoesNotHaveResearch = rebarItemResult != null && e.viewers.none {
            it is Player && it.canCraft(rebarItemResult, true)
        }
        if (anyViewerDoesNotHaveResearch) {
            inventory.result = null
            return
        }
        inventory.result = rebarRecipe.craftingRecipe.result.clone()
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
        var recipe: CraftingRecipeWrapper? = RebarRecipe.searchRecipes(
            RecipeType.VANILLA_SHAPED,
            e.recipe.key,
            RebarRecipe.hashShaped(inventory.contents.toList())
        ) { it.matches(inventory.contents.toList()) }
        if (recipe == null) {
            // Try shapeless instead
            recipe = RebarRecipe.searchRecipes(
                RecipeType.VANILLA_SHAPELESS,
                e.recipe.key,
                RebarRecipe.hashShaped(inventory.contents.toList())
            ) { it.matches(inventory.contents.toList()) }
        }
        if (recipe == null) {
            e.isCancelled = true
            return
        }

        e.result = recipe.craftingRecipe.result.clone()
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun itemInsertEvent(e: InventoryClickEvent) {
        val inventory = e.inventory
        if (inventory is StonecutterInventory) {
            val input = inventory.inputItem ?: return

            if (input.isRebarAndIsNot<VanillaCraftingItem>()) {
                e.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onCook(e: BlockCookEvent) {
        val recipeType = RecipeType.getCookingRecipeTypeByMaterial(e.block.type)
        if (recipeType == null) {
            e.isCancelled = true
            return
        }
        val rebarRecipe = RebarRecipe.searchRecipes(recipeType, e.recipe?.key, e.source.hashIgnoreAmount()) {
            it.matches(e.source)
        }
        if (rebarRecipe != null) {
            e.result = rebarRecipe.recipe.result.clone()
        } else {
            e.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onStartCook(e: FurnaceStartSmeltEvent) {
        val recipeType = RecipeType.getCookingRecipeTypeByMaterial(e.block.type)
        if (recipeType == null) {
            e.totalCookTime = Int.MAX_VALUE
            return
        }
        val rebarRecipe = RebarRecipe.searchRecipes(recipeType, e.recipe.key, e.source.hashIgnoreAmount()) {
            it.matches(e.source)
        }
        if (rebarRecipe == null) {
            e.totalCookTime = Int.MAX_VALUE
        }
    }

    @Suppress("UnstableApiUsage")
    @EventHandler(priority = EventPriority.LOWEST)
    private fun onStartCook(e: CampfireStartEvent) {
        val recipeType = RecipeType.getCookingRecipeTypeByMaterial(e.block.type)
        if (recipeType == null) {
            e.totalCookTime = Int.MAX_VALUE
            return
        }
        val rebarRecipe = RebarRecipe.searchRecipes(recipeType, e.recipe.key, e.source.hashIgnoreAmount()) {
            it.matches(e.source)
        }
        if (rebarRecipe == null) {
            e.totalCookTime = Int.MAX_VALUE
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
        if (input == null) {
            e.isCancelled = true
            return
        }
        val recipeType = RecipeType.getCookingRecipeTypeByMaterial(e.block.type)
        if (recipeType == null) {
            e.isCancelled = true
            return
        }
        val recipe = RebarRecipe.searchRecipes(recipeType, input.hashIgnoreAmount()) { it.matches(input) }
        if (recipe == null) {
            e.isCancelled = true
            return
        }
        // The recipe is already valid because we searched on our end
        val resultSlotItem = furnace.inventory.result
        val canPlaceInOutput = resultSlotItem == null || (recipe.isOutput(resultSlotItem) && resultSlotItem.amount < resultSlotItem.maxStackSize)
        if (!canPlaceInOutput) {
            e.isCancelled = true
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
        val firstRebarItem = RebarItem.fromStack(firstItem)
        val secondRebarItem = RebarItem.fromStack(secondItem)

        // Disallow using Rebar items but allow renaming them
        // Have tried to support this. It's really hard because you end up effectively
        // having to do the repair manually for items that can't usually be repaired.
        // Gave up after 2 hours or so
        if ((firstRebarItem != null && (secondItem != null && !secondItem.isEmpty)) || secondRebarItem != null) {
            e.result = null
            return
        }
    }
}
