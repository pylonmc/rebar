package io.github.pylonmc.rebar.nms.recipe

import net.minecraft.core.registries.Registries
import net.minecraft.world.item.crafting.AbstractCookingRecipe
import net.minecraft.world.item.crafting.CampfireCookingRecipe
import net.minecraft.world.item.crafting.CraftingRecipe
import net.minecraft.world.item.crafting.RecipeHolder
import net.minecraft.world.item.crafting.ShapedRecipePattern
import net.minecraft.world.item.crafting.SmeltingRecipe
import net.minecraft.world.item.crafting.StonecutterRecipe
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.craftbukkit.inventory.CraftRecipe
import org.bukkit.craftbukkit.inventory.trim.CraftTrimPattern
import org.bukkit.craftbukkit.util.CraftNamespacedKey
import org.bukkit.inventory.BlastingRecipe
import org.bukkit.inventory.CampfireRecipe
import org.bukkit.inventory.FurnaceRecipe
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.inventory.SmithingTransformRecipe
import org.bukkit.inventory.SmithingTrimRecipe
import org.bukkit.inventory.SmokingRecipe
import org.bukkit.inventory.StonecuttingRecipe
import org.bukkit.inventory.TransmuteRecipe
import java.util.Objects
import net.minecraft.world.item.crafting.BlastingRecipe as NmsBlastingRecipe
import net.minecraft.world.item.crafting.Recipe as NmsRecipe
import net.minecraft.world.item.crafting.ShapedRecipe as NmsShapedRecipe
import net.minecraft.world.item.crafting.ShapelessRecipe as NmsShapelessRecipe
import net.minecraft.world.item.crafting.SmithingTransformRecipe as NmsSmithingTransformRecipe
import net.minecraft.world.item.crafting.SmithingTrimRecipe as NmsSmithingTrimRecipe
import net.minecraft.world.item.crafting.SmokingRecipe as NmsSmokingRecipe
import net.minecraft.world.item.crafting.TransmuteRecipe as NmsTransmuteRecipe

/**
 * Handles the conversion of bukkit recipes to nms recipes
 * This has to be done manually because any existing conversion will individually
 * convert and automatically register the recipe to the nms recipe manager and reload it.
 *
 * This is a problem because reloading the recipe manager is an expensive operation and
 * doing it for every single recipe will cause a lot of lag.
 *
 * By manually converting the recipes we can register them all at once and reload
 * the recipe manager only once saving up a LOT of tick time.
 *
 * Reference [CraftServer.addRecipe] to keep this up to date w/ version changes
 */
object RecipeMapper {
    fun convertBukkitRecipe(recipe: Recipe): RecipeHolder<NmsRecipe<*>> {
        val nmsRecipe = when (recipe) {
            is ShapedRecipe -> convertShapedRecipe(recipe)
            is ShapelessRecipe -> convertShapelessRecipe(recipe)
            is FurnaceRecipe -> convertFurnaceRecipe(recipe)
            is BlastingRecipe -> convertBlastingRecipe(recipe)
            is CampfireRecipe -> convertCampfireRecipe(recipe)
            is SmokingRecipe -> convertSmokingRecipe(recipe)
            is StonecuttingRecipe -> convertStonecuttingRecipe(recipe)
            is SmithingTransformRecipe -> convertSmithingTransformRecipe(recipe)
            is SmithingTrimRecipe -> convertSmithingTrimRecipe(recipe)
            is TransmuteRecipe -> convertTransmuteRecipe(recipe)
            else -> {
                throw IllegalArgumentException("Unknown recipe type ${recipe.javaClass.canonicalName}, cannot convert to NMS type")
            }
        }
        return RecipeHolder(CraftNamespacedKey.toResourceKey(Registries.RECIPE, recipe.key), nmsRecipe)
    }

    private fun commonInfo() = NmsRecipe.CommonInfo(true)

    private fun convertShapedRecipe(recipe: ShapedRecipe): NmsRecipe<*> {
        val ingredients = recipe.choiceMap.filterValues(Objects::nonNull)
            .mapValues { CraftRecipe.toIngredient(it.value, false) }
        val shape = recipe.shape
        for (i in shape.indices) {
            val row = shape[i]
            shape[i] = row.toCharArray().map { if (it in ingredients) it else ' ' }.joinToString("")
        }
        return NmsShapedRecipe(
            commonInfo(),
            CraftingRecipe.CraftingBookInfo(
                CraftRecipe.getCategory(recipe.category),
                recipe.group
            ),
            ShapedRecipePattern.of(ingredients, *shape),
            CraftItemStack.asTemplate(recipe.result)
        )
    }

    private fun convertShapelessRecipe(recipe: ShapelessRecipe): NmsRecipe<*> {
        val ingredients = recipe.choiceList.map { CraftRecipe.toIngredient(it, false) }
        return NmsShapelessRecipe(
            commonInfo(),
            CraftingRecipe.CraftingBookInfo(
                CraftRecipe.getCategory(recipe.category),
                recipe.group
            ),
            CraftItemStack.asTemplate(recipe.result),
            ingredients
        )
    }

    private fun convertFurnaceRecipe(recipe: FurnaceRecipe): NmsRecipe<*> {
        return SmeltingRecipe(
            commonInfo(),
            AbstractCookingRecipe.CookingBookInfo(
                CraftRecipe.getCategory(recipe.category),
                recipe.group
            ),
            CraftRecipe.toIngredient(recipe.inputChoice, true),
            CraftItemStack.asTemplate(recipe.result),
            recipe.experience,
            recipe.cookingTime
        )
    }

    private fun convertBlastingRecipe(recipe: BlastingRecipe): NmsRecipe<*> {
        return NmsBlastingRecipe(
            commonInfo(),
            AbstractCookingRecipe.CookingBookInfo(
                CraftRecipe.getCategory(recipe.category),
                recipe.group
            ),
            CraftRecipe.toIngredient(recipe.inputChoice, true),
            CraftItemStack.asTemplate(recipe.result),
            recipe.experience,
            recipe.cookingTime
        )
    }

    private fun convertCampfireRecipe(recipe: CampfireRecipe): NmsRecipe<*> {
        return CampfireCookingRecipe(
            commonInfo(),
            AbstractCookingRecipe.CookingBookInfo(
                CraftRecipe.getCategory(recipe.category),
                recipe.group
            ),
            CraftRecipe.toIngredient(recipe.inputChoice, true),
            CraftItemStack.asTemplate(recipe.result),
            recipe.experience,
            recipe.cookingTime
        )
    }

    private fun convertSmokingRecipe(recipe: SmokingRecipe): NmsRecipe<*> {
        return NmsSmokingRecipe(
            commonInfo(),
            AbstractCookingRecipe.CookingBookInfo(
                CraftRecipe.getCategory(recipe.category),
                recipe.group
            ),
            CraftRecipe.toIngredient(recipe.inputChoice, true),
            CraftItemStack.asTemplate(recipe.result),
            recipe.experience,
            recipe.cookingTime
        )
    }

    private fun convertStonecuttingRecipe(recipe: StonecuttingRecipe): NmsRecipe<*> {
        return StonecutterRecipe(
            commonInfo(),
            CraftRecipe.toIngredient(recipe.inputChoice, true),
            CraftItemStack.asTemplate(recipe.result)
        )
    }

    private fun convertSmithingTransformRecipe(recipe: SmithingTransformRecipe): NmsRecipe<*> {
        return NmsSmithingTransformRecipe(
            commonInfo(),
            CraftRecipe.toPossibleIngredient(recipe.template, false),
            CraftRecipe.toIngredient(recipe.base, false),
            CraftRecipe.toPossibleIngredient(recipe.addition, false),
            CraftItemStack.asTemplate(recipe.result),
            recipe.willCopyDataComponents()
        )
    }

    private fun convertSmithingTrimRecipe(recipe: SmithingTrimRecipe): NmsRecipe<*> {
        return NmsSmithingTrimRecipe(
            commonInfo(),
            CraftRecipe.toIngredient(recipe.template, false),
            CraftRecipe.toIngredient(recipe.base, false),
            CraftRecipe.toIngredient(recipe.addition, false),
            CraftTrimPattern.bukkitToMinecraftHolder(recipe.trimPattern),
            recipe.willCopyDataComponents()
        )
    }

    private fun convertTransmuteRecipe(recipe: TransmuteRecipe): NmsRecipe<*> {
        return NmsTransmuteRecipe(
            commonInfo(),
            CraftingRecipe.CraftingBookInfo(
                CraftRecipe.getCategory(recipe.category),
                recipe.group
            ),
            CraftRecipe.toIngredient(recipe.input, true),
            CraftRecipe.toIngredient(recipe.material, true),
            NmsTransmuteRecipe.DEFAULT_MATERIAL_COUNT,
            CraftItemStack.asTemplate(recipe.result),
            false
        )
    }

}