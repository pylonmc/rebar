package io.github.pylonmc.rebar.recipe.vanilla

import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.item.ItemTypeWrapper
import io.github.pylonmc.rebar.nms.NmsAccessor
import io.github.pylonmc.rebar.recipe.ConfigurableRecipeType
import io.github.pylonmc.rebar.recipe.ingredient.ItemChoice
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

sealed interface BukkitRebarRecipe : RebarRecipe {
    val bukkitRecipe: Recipe
}

sealed interface DummyBukkitRebarRecipe : BukkitRebarRecipe {
    companion object {
        fun dummyKey(originalKey: NamespacedKey): NamespacedKey {
            return NamespacedKey(originalKey.namespace, "internal_dummy_" + originalKey.key)
        }

        fun recipeKey(originalKey: NamespacedKey): NamespacedKey {
            val dummyKey = dummyKey(originalKey)
            return if (RecipeType.isDummyRecipe(dummyKey)) dummyKey else originalKey
        }
    }
}

sealed class BukkitRecipeType<T: BukkitRebarRecipe>(key: NamespacedKey) : ConfigurableRecipeType<T>(key) {
    override fun addRecipe(recipe: T) {
        super.addRecipe(recipe)
        if (NmsAccessor.instance.hasRecipe(recipe.key)) {
            NmsAccessor.queueUnregisterRecipe(recipe.key)
        }
        NmsAccessor.queueRegisterRecipe(recipe.bukkitRecipe)
    }

    override fun removeRecipe(recipe: NamespacedKey) {
        super.removeRecipe(recipe)
        NmsAccessor.queueUnregisterRecipe(recipe)
    }
}

sealed class DummyRecipeType<T: BukkitRebarRecipe>(key: NamespacedKey) : BukkitRecipeType<T>(key) {
    override fun loadRecipe(key: NamespacedKey, section: ConfigSection): T {
        throw IllegalAccessException("DummyRecipeType should not be loaded from config")
    }

    fun removeDummyRecipeFor(recipe: NamespacedKey) = removeRecipe(DummyBukkitRebarRecipe.dummyKey(recipe))
}

sealed class VanillaRecipeType<T : BukkitRebarRecipe, D : DummyBukkitRebarRecipe>(
    key: String,
    val dummyType: DummyRecipeType<D>
) : BukkitRecipeType<T>(NamespacedKey.minecraft(key)) {
    abstract fun createDummyRecipeFor(recipe: T): D

    override fun addRecipe(recipe: T) {
        super.addRecipe(recipe)
        dummyType.addRecipe(createDummyRecipeFor(recipe))
    }

    override fun removeRecipe(recipe: NamespacedKey) {
        super.removeRecipe(recipe)
        dummyType.removeDummyRecipeFor(recipe)
    }

    @JvmSynthetic
    internal fun addNonRebarRecipe(recipe: T) {
        registeredRecipes[recipe.key] = recipe
        nonRebarRecipes.add(recipe.key)
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
val CookingRecipe<*>.rebarRecipeType: VanillaRecipeType<out CookingRebarRecipe, DummyCookingRebarRecipe>?
    get() = when (this) {
        is BlastingRecipe -> BlastingRecipeType
        is CampfireRecipe -> CampfireRecipeType
        is FurnaceRecipe -> FurnaceRecipeType
        is SmokingRecipe -> SmokingRecipeType
        else -> null
    }