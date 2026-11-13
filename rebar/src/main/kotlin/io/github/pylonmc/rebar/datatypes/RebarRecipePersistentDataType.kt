package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.recipe.RebarRecipe
import io.github.pylonmc.rebar.recipe.RecipeType
import org.bukkit.NamespacedKey

class RebarRecipePersistentDataType<T : RebarRecipe>(private val clazz: Class<T>, val recipeType: RecipeType<T>) :
    DelegatingPersistentDataType<String, NamespacedKey, T>(RebarSerializers.NAMESPACED_KEY) {

    override fun getComplexType() = clazz

    override fun toDelegatedType(complex: T) = complex.key

    override fun fromDelegatedType(primitive: NamespacedKey) = recipeType.getRecipeOrThrow(primitive)

    companion object {

        @JvmStatic
        fun <T : RebarRecipe> recipeTypeFrom(clazz: Class<T>, recipeType: RecipeType<T>) = RebarRecipePersistentDataType(clazz, recipeType)

        @JvmSynthetic
        inline fun <reified T : RebarRecipe> recipeTypeFrom(recipeType: RecipeType<T>) = recipeTypeFrom(T::class.java, recipeType)
    }
}