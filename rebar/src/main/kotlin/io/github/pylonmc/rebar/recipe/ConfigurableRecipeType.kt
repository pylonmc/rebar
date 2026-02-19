package io.github.pylonmc.rebar.recipe

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import io.github.pylonmc.rebar.recipe.RebarRecipe.Companion.priority
import org.bukkit.NamespacedKey

abstract class ConfigurableRecipeType<T : RebarRecipe>(key: NamespacedKey) : RecipeType<T>(key) {

    @JvmSynthetic
    internal val filePath = "recipes/${key.namespace}/${key.key}.yml"

    open fun loadFromConfig(config: ConfigSection) {
        for (key in config.keys) {
            val section = config.getSectionOrThrow(key)
            val key = NamespacedKey.fromString(key) ?: error("Invalid key: $key")
            try {
                val recipe = loadRecipe(key, section)
                val priority = section.get("priority", ConfigAdapter.DOUBLE)
                if (priority != null) {
                    recipe.priority = priority
                }
                addRecipe(recipe)
            } catch (e: Exception) {
                Rebar.logger.severe("Failed to load recipe with key '$key' from config for recipe type ${this.key}")
                e.printStackTrace()
            }
        }
    }

    protected abstract fun loadRecipe(key: NamespacedKey, section: ConfigSection): T
}