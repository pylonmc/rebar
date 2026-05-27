package io.github.pylonmc.rebar.nms.recipe

import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeHolder
import net.minecraft.world.item.crafting.RecipeInput
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraft.world.item.crafting.RecipeType
import java.util.Optional

/**
 * A dedicated class for [RecipeManager.CachedCheck] that allows changing the lastRecipe field
 * This is effectively identical to the result of [RecipeManager.createCheck] except it's not
 * an anonymous class
 *
 * This is necessary to avoid using laggy reflection on the anonymous class provided by [RecipeManager.createCheck]
 */
class AccessibleCachedCheck<I : RecipeInput, T : Recipe<I>>(
    val type: RecipeType<T>,
    var lastRecipe: ResourceKey<Recipe<*>>? = null
) : RecipeManager.CachedCheck<I, T> {

    override fun getRecipeFor(input: I, level: ServerLevel): Optional<RecipeHolder<T>> {
        val recipeManager = level.recipeAccess()
        val result = recipeManager.getRecipeFor(type, input, level, this.lastRecipe)
        if (result.isPresent) {
            this.lastRecipe = result.get().id
        }
        return result
    }

}