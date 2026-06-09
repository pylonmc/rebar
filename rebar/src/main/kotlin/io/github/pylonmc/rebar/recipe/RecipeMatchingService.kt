package io.github.pylonmc.rebar.recipe

import io.github.pylonmc.rebar.recipe.vanilla.AbstractCraftingRebarRecipe
import io.github.pylonmc.rebar.recipe.vanilla.CraftingInput
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.inventory.TransmuteRecipe

object RecipeMatchingService {

    fun matchCraftingRecipe(input: CraftingInput, possibleRecipe: Recipe? = null, lastRecipe: AbstractCraftingRebarRecipe? = null): AbstractCraftingRebarRecipe? {
        if (possibleRecipe != null) {
            val possibleRebarRecipe = when(possibleRecipe) {
                is ShapedRecipe -> RecipeType.VANILLA_SHAPED.getRecipe(possibleRecipe.key)
                is ShapelessRecipe -> RecipeType.VANILLA_SHAPELESS.getRecipe(possibleRecipe.key)
                is TransmuteRecipe -> RecipeType.VANILLA_TRANSMUTE.getRecipe(possibleRecipe.key)
                else -> null
            }

            if (possibleRebarRecipe != null && possibleRebarRecipe.matches(input)) {
                return possibleRebarRecipe
            }
        }

        if (lastRecipe != null && lastRecipe.matches(input)) {
            return lastRecipe
        }

        for (recipe in RecipeType.vanillaCraftingRecipes()) {
            if (recipe.matches(input)) {
                return recipe
            }
        }
        return null
    }
}