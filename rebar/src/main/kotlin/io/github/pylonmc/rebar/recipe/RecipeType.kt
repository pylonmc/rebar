package io.github.pylonmc.rebar.recipe

import io.github.pylonmc.rebar.recipe.vanilla.*
import io.github.pylonmc.rebar.registry.RebarRegistry
import org.bukkit.Bukkit
import org.bukkit.Keyed
import org.bukkit.NamespacedKey
import org.bukkit.inventory.*

/**
 * Serves as a registry and container for recipes of a specific type.
 *
 * Iteration order will be the order in which recipes were added unless overridden. You should
 * never assume that the list of recipes is static, as recipes may be added or removed at any time.
 */
open class RecipeType<T : RebarRecipe>(private val key: NamespacedKey) : Keyed, Iterable<T> {

    protected open val registeredRecipes = LinkedHashMap<NamespacedKey, T>()
    val recipes: Collection<T>
        get() = registeredRecipes.values

    fun getRecipe(key: NamespacedKey): T? = registeredRecipes[key]

    fun getRecipeOrThrow(key: NamespacedKey): T {
        return registeredRecipes[key] ?: throw NoSuchElementException("No recipe found for key $key in ${this.key}")
    }

    open fun addRecipe(recipe: T) {
        registeredRecipes[recipe.key] = recipe
    }

    open fun removeRecipe(recipe: NamespacedKey) {
        registeredRecipes.remove(recipe)
    }

    fun register() {
        RebarRegistry.RECIPE_TYPES.register(this)
    }

    fun stream() = registeredRecipes.values.stream()

    override fun iterator(): Iterator<T> = registeredRecipes.values.iterator()

    override fun getKey(): NamespacedKey = key

    companion object {
        /**
         * Key: `minecraft:blasting`
         */
        @JvmField
        val VANILLA_BLASTING = BlastingRecipeType

        /**
         * Key: `minecraft:campfire_cooking`
         */
        @JvmField
        val VANILLA_CAMPFIRE = CampfireRecipeType

        /**
         * Key: `minecraft:smelting`
         */
        @JvmField
        val VANILLA_FURNACE = FurnaceRecipeType

        /**
         * Key: `minecraft:crafting_shaped`
         */
        @JvmField
        val VANILLA_SHAPED = ShapedRecipeType

        /**
         * Key: `minecraft:crafting_shapeless`
         */
        @JvmField
        val VANILLA_SHAPELESS = ShapelessRecipeType

        /**
         * Key: `minecraft:crafting_transmute`
         */
        @JvmField
        val VANILLA_TRANSMUTE = TransmuteRecipeType

        /**
         * Key: `minecraft:smithing_transform`
         */
        @JvmField
        val VANILLA_SMITHING_TRANSFORM = SmithingTransformRecipeType

        /**
         * Key: `minecraft:smithing_trim`
         */
        @JvmField
        val VANILLA_SMITHING_TRIM = SmithingTrimRecipeType

        /**
         * Key: `minecraft:smoking`
         */
        @JvmField
        val VANILLA_SMOKING = SmokingRecipeType

        init {
            VANILLA_BLASTING.register()
            VANILLA_CAMPFIRE.register()
            VANILLA_FURNACE.register()
            VANILLA_SHAPED.register()
            VANILLA_SHAPELESS.register()
            VANILLA_SMITHING_TRANSFORM.register()
            VANILLA_SMITHING_TRIM.register()
            VANILLA_SMOKING.register()
        }

        @JvmStatic
        fun vanillaCraftingRecipes() = VANILLA_SHAPED
            .union(VANILLA_SHAPELESS)
            .union(VANILLA_TRANSMUTE)

        @JvmStatic
        fun vanillaCookingRecipes() = VANILLA_BLASTING.recipes
            .union(VANILLA_CAMPFIRE.recipes)
            .union(VANILLA_FURNACE.recipes)
            .union(VANILLA_SMOKING.recipes)

        @JvmSynthetic
        internal fun addVanillaRecipes() {
            for (recipe in Bukkit.recipeIterator()) {
                // @formatter:off
                when (recipe) {
                    is BlastingRecipe -> VANILLA_BLASTING.addNonRebarRecipe(BlastingRecipeWrapper(recipe))
                    is CampfireRecipe -> VANILLA_CAMPFIRE.addNonRebarRecipe(CampfireRecipeWrapper(recipe))
                    is FurnaceRecipe -> VANILLA_FURNACE.addNonRebarRecipe(FurnaceRecipeWrapper(recipe))
                    is ShapedRecipe -> VANILLA_SHAPED.addNonRebarRecipe(ShapedRecipeWrapper(recipe))
                    is ShapelessRecipe -> VANILLA_SHAPELESS.addNonRebarRecipe(ShapelessRecipeWrapper(recipe))
                    is TransmuteRecipe -> VANILLA_TRANSMUTE.addNonRebarRecipe(TransmuteRecipeWrapper(recipe))
                    is SmithingTrimRecipe -> VANILLA_SMITHING_TRIM.addNonRebarRecipe(SmithingTrimRecipeWrapper(recipe))
                    is SmithingTransformRecipe -> VANILLA_SMITHING_TRANSFORM.addNonRebarRecipe(SmithingTransformRecipeWrapper(recipe))
                    is SmokingRecipe -> VANILLA_SMOKING.addNonRebarRecipe(SmokingRecipeWrapper(recipe))
                }
                // @formatter:on
            }
        }
    }
}