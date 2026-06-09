package io.github.pylonmc.rebar.recipe.logic

import io.github.pylonmc.rebar.recipe.RecipeType
import io.github.pylonmc.rebar.recipe.vanilla.AbstractCraftingRebarRecipe
import io.github.pylonmc.rebar.recipe.vanilla.CookingRebarRecipe
import io.github.pylonmc.rebar.recipe.vanilla.CraftingInput
import io.github.pylonmc.rebar.recipe.vanilla.SmithingRebarRecipe
import io.github.pylonmc.rebar.recipe.vanilla.VanillaRecipeType
import org.bukkit.Keyed
import org.bukkit.inventory.BlastingRecipe
import org.bukkit.inventory.CampfireRecipe
import org.bukkit.inventory.CookingRecipe
import org.bukkit.inventory.FurnaceRecipe
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.inventory.SmithingTransformRecipe
import org.bukkit.inventory.SmithingTrimRecipe
import org.bukkit.inventory.SmokingRecipe
import org.bukkit.inventory.TransmuteRecipe

object RecipeMatchingService {
    fun matchCraftingRecipe(input: CraftingInput, possibleRecipe: Recipe? = null, lastRecipe: AbstractCraftingRebarRecipe? = null): AbstractCraftingRebarRecipe? {
        if (possibleRecipe != null) {
            var possibleRebarRecipe = when(possibleRecipe) {
                is ShapedRecipe -> RecipeType.VANILLA_SHAPED.getRecipe(possibleRecipe.key)
                is ShapelessRecipe -> RecipeType.VANILLA_SHAPELESS.getRecipe(possibleRecipe.key)
                is TransmuteRecipe -> RecipeType.VANILLA_TRANSMUTE.getRecipe(possibleRecipe.key)
                else -> null
            }

            if (possibleRebarRecipe == null && possibleRecipe is Keyed) {
                val dummyRecipe = RecipeType.DUMMY_CRAFTING.getRecipe(possibleRecipe.key)
                if (dummyRecipe != null) {
                    possibleRebarRecipe = dummyRecipe.realRecipe
                }
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

    fun matchCookingRecipe(type: VanillaRecipeType<out CookingRebarRecipe>, input: ItemStack?, output: ItemStack?, possibleRecipe: CookingRecipe<*>?, lastRecipe: CookingRebarRecipe? = null): CookingRebarRecipe? {
        if (possibleRecipe != null) {
            var possibleRebarRecipe = when(possibleRecipe) {
                is CampfireRecipe -> RecipeType.VANILLA_CAMPFIRE.getRecipe(possibleRecipe.key)
                is FurnaceRecipe -> RecipeType.VANILLA_FURNACE.getRecipe(possibleRecipe.key)
                is SmokingRecipe -> RecipeType.VANILLA_SMOKING.getRecipe(possibleRecipe.key)
                is BlastingRecipe -> RecipeType.VANILLA_BLASTING.getRecipe(possibleRecipe.key)
                else -> null
            }
            if (possibleRebarRecipe == null) {
                val dummyRecipe = RecipeType.DUMMY_COOKING.getRecipe(possibleRecipe.key)
                if (dummyRecipe != null) {
                    possibleRebarRecipe = dummyRecipe.realRecipe
                }
            }

            if (possibleRebarRecipe != null && possibleRebarRecipe.matches(input, output)) {
                return possibleRebarRecipe
            }
        }

        if (lastRecipe != null && lastRecipe.matches(input, output)) {
            return lastRecipe
        }

        for (recipe in type) {
            if (recipe.matches(input, output)) {
                return recipe
            }
        }
        return null
    }

    fun matchSmithingRecipe(template: ItemStack?, base: ItemStack?, addition: ItemStack?, possibleRecipe: Recipe?, lastRecipe: SmithingRebarRecipe? = null): SmithingRebarRecipe? {
        if (possibleRecipe != null) {
            var possibleRebarRecipe = when(possibleRecipe) {
                is SmithingTransformRecipe -> RecipeType.VANILLA_SMITHING_TRANSFORM.getRecipe(possibleRecipe.key)
                is SmithingTrimRecipe -> RecipeType.VANILLA_SMITHING_TRIM.getRecipe(possibleRecipe.key)
                else -> null
            }

            if (possibleRebarRecipe == null && possibleRecipe is Keyed) {
                val dummyRecipe = RecipeType.DUMMY_SMITHING.getRecipe(possibleRecipe.key)
                if (dummyRecipe != null) {
                    possibleRebarRecipe = dummyRecipe.realRecipe
                }
            }

            if (possibleRebarRecipe != null && possibleRebarRecipe.matches(template, base, addition)) {
                return possibleRebarRecipe
            }
        }

        if (lastRecipe != null && lastRecipe.matches(template, base, addition)) {
            return lastRecipe
        }

        for (recipe in RecipeType.vanillaSmithingRecipes()) {
            if (recipe.matches(template, base, addition)) {
                return recipe
            }
        }
        return null
    }
}