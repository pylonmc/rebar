package io.github.pylonmc.rebar.recipe.vanilla

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.item.ItemTypeWrapper
import io.github.pylonmc.rebar.recipe.ConfigurableRecipeType
import io.github.pylonmc.rebar.recipe.RebarRecipe
import io.github.pylonmc.rebar.recipe.RecipeInput
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

sealed interface VanillaRecipeWrapper : RebarRecipe {
    val recipe: Recipe
}

sealed class VanillaRecipeType<T : VanillaRecipeWrapper>(key: String) :
    ConfigurableRecipeType<T>(NamespacedKey.minecraft(key)), Listener {

    init {
        Bukkit.getPluginManager().registerEvents(this, Rebar)
    }

    override fun addRecipe(recipe: T) {
        super.addRecipe(recipe)
        if (Bukkit.getRecipe(recipe.key) != null) {
            Bukkit.removeRecipe(recipe.key)
        }
        Bukkit.addRecipe(recipe.recipe)
    }

    @JvmSynthetic
    internal fun addNonRebarRecipe(recipe: T) {
        registeredRecipes[recipe.key] = recipe
        nonRebarRecipes.add(recipe.key)
    }

    override fun removeRecipe(recipe: NamespacedKey) {
        super.removeRecipe(recipe)
        Bukkit.removeRecipe(recipe)
    }

    companion object {
        @JvmSynthetic
        internal val nonRebarRecipes: MutableSet<NamespacedKey> = mutableSetOf()
    }
}

@JvmSynthetic
internal fun RecipeChoice.asRecipeInput(): RecipeInput {
    return when (this) {
        is RecipeChoice.ExactChoice -> RecipeInput.Item(
            this.itemStack.amount,
            *this.choices.toTypedArray()
        )

        is RecipeChoice.MaterialChoice -> RecipeInput.Item(
            this.choices.mapTo(mutableListOf()) { ItemTypeWrapper(it) },
            1
        )

        else -> throw IllegalArgumentException("Unsupported RecipeChoice type: ${this::class.java.name}")
    }
}

@JvmSynthetic
internal fun RecipeInput.Item.asRecipeChoice(): RecipeChoice {
    return RecipeChoice.ExactChoice(representativeItems.mapTo(mutableListOf()) { it.clone() })
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