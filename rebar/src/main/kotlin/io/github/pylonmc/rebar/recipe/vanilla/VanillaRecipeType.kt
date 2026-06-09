package io.github.pylonmc.rebar.recipe.vanilla

import io.github.pylonmc.rebar.item.ItemTypeWrapper
import io.github.pylonmc.rebar.nms.NmsAccessor
import io.github.pylonmc.rebar.recipe.ConfigurableRecipeType
import io.github.pylonmc.rebar.recipe.ItemChoice
import io.github.pylonmc.rebar.recipe.RebarRecipe
import io.github.pylonmc.rebar.recipe.RecipeType
import org.bukkit.NamespacedKey
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

sealed interface DummyVanillaRebarRecipe : VanillaRebarRecipe {
    companion object {
        fun dummyKey(originalKey: NamespacedKey): NamespacedKey {
            return NamespacedKey(originalKey.namespace, "internal_dummy_" + originalKey.key)
        }

        fun recipeKey(originalKey: NamespacedKey): NamespacedKey {
            val dummyKey = dummyKey(originalKey)
            if (RecipeType.isDummyRecipe(dummyKey)) {
                return dummyKey
            }
            return originalKey
        }
    }
}

sealed class DummyRecipeType<T: VanillaRebarRecipe>(key: NamespacedKey) : RecipeType<T>(key) {
    override fun addRecipe(recipe: T) {
        super.addRecipe(recipe)
        if (NmsAccessor.instance.hasRecipe(recipe.key)) {
            NmsAccessor.queueUnregisterRecipe(recipe.key)
        }
        NmsAccessor.queueRegisterRecipe(recipe.recipe)
    }

    fun removeDummyRecipeFor(recipe: NamespacedKey) = removeRecipe(DummyVanillaRebarRecipe.dummyKey(recipe))
}

sealed class VanillaRecipeType<T : VanillaRebarRecipe>(key: String) : ConfigurableRecipeType<T>(NamespacedKey.minecraft(key)) {
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
val CookingRecipe<*>.rebarRecipeType: VanillaRecipeType<out CookingRebarRecipe>?
    get() = when (this) {
        is BlastingRecipe -> BlastingRecipeType
        is CampfireRecipe -> CampfireRecipeType
        is FurnaceRecipe -> FurnaceRecipeType
        is SmokingRecipe -> SmokingRecipeType
        else -> null
    }