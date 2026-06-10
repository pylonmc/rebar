package io.github.pylonmc.rebar.recipe.logic

import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.recipe.RebarRecipe
import io.github.pylonmc.rebar.recipe.RecipeType
import io.github.pylonmc.rebar.recipe.ingredient.FluidOrItemChoice
import io.github.pylonmc.rebar.recipe.vanilla.CraftingRebarRecipe
import io.github.pylonmc.rebar.recipe.vanilla.CookingRebarRecipe
import io.github.pylonmc.rebar.recipe.vanilla.CraftingInput
import io.github.pylonmc.rebar.recipe.vanilla.DummyCookingRebarRecipe
import io.github.pylonmc.rebar.recipe.vanilla.SmithingRebarRecipe
import io.github.pylonmc.rebar.recipe.vanilla.VanillaRecipeType
import org.bukkit.Keyed
import org.bukkit.Material
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
import org.bukkit.inventory.CraftingInventory
import org.bukkit.inventory.SmithingInventory
import org.bukkit.event.inventory.FurnaceStartSmeltEvent
import org.bukkit.event.block.BlockCookEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent

/**
 * A service providing qol methods for matching against rebar's vanilla recipe replacement.
 *
 * Minecraft's recipe choice system is incredibly shallow, by default you can only ever
 * match by [item type][Material] or by exact [ItemStack] equality. This is okay enough for vanilla & datapacks,
 * but the moment you add "custom" items that use vanilla types, it quickly falls short.
 *
 * Because of this Rebar replaces vanilla recipe matching with our own [FluidOrItemChoice] system,
 * allowing for much more flexible and dynamic recipe matching.
 *
 * The basic logic of which is contained in this class.
 *
 * Currently supports:
 * - [matchCraftingRecipe] for crafting tables, crafters, etc.
 * - [matchCookingRecipe] for furnaces, smokers, campfires, etc.
 * - [matchSmithingRecipe] for the smithing table.
 *
 * @see RebarRecipeListener
 */
object RecipeMatchingService {
    /**
     * Takes in a [CraftingInput] and matches it (if possible) to a [CraftingRebarRecipe].
     *
     * [possibleRecipe] typically comes from [PrepareItemCraftEvent.getRecipe] or [CraftingInventory.getRecipe],
     * It's what Minecraft thinks the correct recipe is, but because [RebarItem]s use vanilla ingredients
     * and [RebarRecipe]s use our custom [FluidOrItemChoice], it may not always be correct.
     *
     * If you are using [matchCraftingRecipe] for a machine or something similar, storing the last matched recipe and passing
     * it in as [lastRecipe] can speed up matching significantly, as the same recipe is often used multiple times in a row.
     *
     * @param input The [CraftingInput] to match with
     * @param possibleRecipe The possible bukkit [Recipe] that may or may not match [CraftingInput], tried before all other recipes
     * @param lastRecipe The last recipe matched if applicable, this will be checked after [possibleRecipe] but before all others
     *
     * @return The matched [CraftingRebarRecipe] or null if none was found
     */
    fun matchCraftingRecipe(input: CraftingInput, possibleRecipe: Recipe? = null, lastRecipe: CraftingRebarRecipe? = null): CraftingRebarRecipe? {
        if (possibleRecipe != null) {
            var possibleRebarRecipe = when(possibleRecipe) {
                is ShapedRecipe -> RecipeType.VANILLA_SHAPED.getRecipe(possibleRecipe.key)
                is ShapelessRecipe -> RecipeType.VANILLA_SHAPELESS.getRecipe(possibleRecipe.key)
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

    /**
     * Takes in an input and output [ItemStack] and matches it (if possible) to a [CookingRebarRecipe] of a specific [type].
     * Because cooking blocks have an output buffer slot, we need to make sure the output matches so that it can fit.
     *
     * [possibleRecipe] typically comes from [FurnaceStartSmeltEvent.getRecipe] or [BlockCookEvent.getRecipe],
     * It's what Minecraft thinks the correct recipe is, but because [RebarItem]s use vanilla ingredients
     * and [RebarRecipe]s use our custom [FluidOrItemChoice], it may not always be correct.
     *
     * If you are using [matchCraftingRecipe] for a machine or something similar, storing the last matched recipe and passing
     * it in as [lastRecipe] can speed up matching significantly, as the same recipe is often used multiple times in a row.
     *
     * @param type The type of cooking recipe to match against (ex: furnace, smoker, campfire, etc.)
     * @param input The input [ItemStack] to match against (ex: the top slot in a furnace)
     * @param output The output [ItemStack] to match against (ex: the right slot in a furnace)
     * @param possibleRecipe The possible bukkit [Recipe] that may or may not match [input] and [output], tried before all other recipes
     * @param lastRecipe The last recipe matched if applicable, this will be checked after [possibleRecipe] but before all others
     *
     * @return The matched [CookingRebarRecipe] or null if none was found
     */
    fun matchCookingRecipe(type: VanillaRecipeType<out CookingRebarRecipe, DummyCookingRebarRecipe>, input: ItemStack?, output: ItemStack?, possibleRecipe: CookingRecipe<*>?, lastRecipe: CookingRebarRecipe? = null): CookingRebarRecipe? {
        if (possibleRecipe != null) {
            var possibleRebarRecipe = when(possibleRecipe) {
                is CampfireRecipe -> RecipeType.VANILLA_CAMPFIRE.getRecipe(possibleRecipe.key)
                is FurnaceRecipe -> RecipeType.VANILLA_SMELTING.getRecipe(possibleRecipe.key)
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

    /**
     * Takes in a [template] [base] and [addition] and matches them (if possible) to a [SmithingRebarRecipe].
     *
     * [possibleRecipe] typically comes from [SmithingInventory.getRecipe],
     * It's what Minecraft thinks the correct recipe is, but because [RebarItem]s use vanilla ingredients
     * and [RebarRecipe]s use our custom [FluidOrItemChoice], it may not always be correct.
     *
     * If you are using [matchCraftingRecipe] for a machine or something similar, storing the last matched recipe and passing
     * it in as [lastRecipe] can speed up matching significantly, as the same recipe is often used multiple times in a row.
     *
     * @param template The template [ItemStack] to match against (or null if there isn't one)
     * @param base The base [ItemStack] to match against (or null if there isn't one)
     * @param addition The addition [ItemStack] to match against (or null if there isn't one)
     * @param possibleRecipe The possible bukkit [Recipe] that may or may not match the [template] [base], and [addition], tried before all other recipes
     * @param lastRecipe The last recipe matched if applicable, this will be checked after [possibleRecipe] but before all others
     *
     * @return The matched [SmithingRebarRecipe] or null if none was found
     */
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