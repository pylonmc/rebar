package io.github.pylonmc.rebar.recipe.vanilla

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.item.ItemTypeWrapper
import io.github.pylonmc.rebar.nms.NmsAccessor
import io.github.pylonmc.rebar.recipe.ConfigurableRecipeType
import io.github.pylonmc.rebar.recipe.ItemChoice
import io.github.pylonmc.rebar.recipe.RebarRecipe
import io.github.pylonmc.rebar.recipe.RecipeType
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.event.Listener
import org.bukkit.inventory.BlastingRecipe
import org.bukkit.inventory.CampfireRecipe
import org.bukkit.inventory.CookingRecipe
import org.bukkit.inventory.FurnaceRecipe
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.SmokingRecipe

sealed interface VanillaRebarRecipe : RebarRecipe {
    val recipe: Recipe
}

sealed class VanillaRecipeType<T : VanillaRebarRecipe>(key: String) : ConfigurableRecipeType<T>(NamespacedKey.minecraft(key)), Listener {

    init {
        Bukkit.getPluginManager().registerEvents(this, Rebar)
    }

    override fun addRecipe(recipe: T) {
        super.addRecipe(recipe)
        if (NmsAccessor.instance.hasRecipe(recipe.key)) {
            NmsAccessor.queueUnregisterRecipe(recipe.key)
        }
        NmsAccessor.queueRegisterRecipe(recipe.recipe)
    }

    @JvmSynthetic
    internal fun addNonRebarRecipe(recipe: T) {
        registeredRecipes[recipe.key] = recipe
        nonRebarRecipes.add(recipe.key)
    }

    override fun removeRecipe(recipe: NamespacedKey) {
        super.removeRecipe(recipe)
        NmsAccessor.queueUnregisterRecipe(recipe)
    }

    companion object {
        @JvmSynthetic
        internal val nonRebarRecipes: MutableSet<NamespacedKey> = mutableSetOf()
    }
}

@JvmSynthetic
internal fun RecipeChoice.toItemChoice(): ItemChoice {
    return when (this) {
        is RecipeChoice.ExactChoice -> ItemChoice.Builder().apply {
            for (choice in this@toItemChoice.choices) {
                addExact(choice)
            }
        }.build()

        is RecipeChoice.MaterialChoice -> ItemChoice.Builder().apply {
            for (choice in this@toItemChoice.choices) {
                addFuzzy(ItemTypeWrapper(choice))
            }
        }.build()

        else -> throw IllegalArgumentException("Unsupported RecipeChoice type: ${this::class.java.name}")
    }
}

@get:JvmSynthetic
val CookingRecipe<*>.recipeType: RecipeType<*>?
    get() = when (this) {
        is BlastingRecipe -> BlastingRecipeType
        is CampfireRecipe -> CampfireRecipeType
        is FurnaceRecipe -> FurnaceRecipeType
        is SmokingRecipe -> SmokingRecipeType
        else -> null
    }