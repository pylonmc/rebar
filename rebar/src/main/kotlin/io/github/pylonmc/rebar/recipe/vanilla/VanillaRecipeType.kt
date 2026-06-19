package io.github.pylonmc.rebar.recipe.vanilla

import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.item.ItemTypeWrapper
import io.github.pylonmc.rebar.nms.NmsAccessor
import io.github.pylonmc.rebar.recipe.ConfigurableRecipeType
import io.github.pylonmc.rebar.recipe.ingredient.ItemChoice
import io.github.pylonmc.rebar.recipe.RebarRecipe
import io.github.pylonmc.rebar.recipe.RecipeType
import io.github.pylonmc.rebar.recipe.logic.RecipeMatchingService
import io.github.pylonmc.rebar.recipe.logic.RebarRecipeListener
import org.bukkit.NamespacedKey
import org.bukkit.inventory.BlastingRecipe
import org.bukkit.inventory.CampfireRecipe
import org.bukkit.inventory.CookingRecipe
import org.bukkit.inventory.FurnaceRecipe
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.SmokingRecipe

/**
 * A [RebarRecipe] with a bukkit [Recipe] that needs to be (un)registered
 * alongside this recipe.
 *
 * @see CraftingRebarRecipe
 * @see CookingRebarRecipe
 * @see SmithingRebarRecipe
 */
sealed interface BukkitRebarRecipe : RebarRecipe {
    val bukkitRecipe: Recipe
}

/**
 * A dummy version of the real [BukkitRebarRecipe], these will always use
 * [RecipeChoice.MaterialChoice] so that vanilla minecraft can match against
 * the recipe before we correct it in [RecipeMatchingService]
 *
 * @see DummyCraftingRebarRecipe
 * @see DummyCookingRebarRecipe
 * @see DummySmithingRebarRecipe
 */
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

/**
 * A [ConfigurableRecipeType] that automatically handles (un)registering of its recipes' bukkit counterparts.
 */
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

/**
 * [BukkitRecipeType] but for [DummyBukkitRebarRecipe]s, these are never loaded from config and
 * instead are only ever created by [VanillaRecipeType.createDummyRecipeFor] when a real recipe is added,
 * and removed when the real recipe is removed.
 *
 * @see DummyCraftingRecipeType
 * @see DummyCookingRecipeType
 * @see DummySmithingRecipeType
 */
sealed class DummyRecipeType<T: DummyBukkitRebarRecipe>(key: NamespacedKey) : BukkitRecipeType<T>(key) {
    override fun loadRecipe(key: NamespacedKey, section: ConfigSection): T {
        throw IllegalAccessException("DummyRecipeType should not be loaded from config")
    }

    fun removeDummyRecipeFor(recipe: NamespacedKey) = removeRecipe(DummyBukkitRebarRecipe.dummyKey(recipe))
}

/**
 * A wrapper around a vanilla recipe type of some kind. (Crafting, Smelting, etc.)
 *
 * All vanilla recipe's are backed by a [dummy recipe][DummyBukkitRebarRecipe] automatically created
 * whenever a [a recipe is added][addRecipe], and removed whenever a recipe is removed.
 *
 * Without dummy recipes blocks like the furnace would be unable to process our custom recipes
 * as we cannot pass [ItemChoice] to the vanilla implementation. Therefor we use
 * dummy recipes to start the recipe, and correct them in [RebarRecipeListener] using [RecipeMatchingService]
 *
 * @param key The vanilla id for this recipe type (for ex: `crafting_shaped`)
 * @param dummyType The dummy type for this recipe type
 *
 * @see ShapedRecipeType
 * @see ShapelessRecipeType
 * @see SmeltingRecipeType
 * @see CampfireRecipeType
 * @see SmithingTransformRecipeType
 */
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
        is FurnaceRecipe -> SmeltingRecipeType
        is SmokingRecipe -> SmokingRecipeType
        else -> null
    }