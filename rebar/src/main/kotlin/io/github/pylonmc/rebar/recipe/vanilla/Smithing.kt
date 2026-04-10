package io.github.pylonmc.rebar.recipe.vanilla

import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import io.github.pylonmc.rebar.guide.button.ItemButton
import io.github.pylonmc.rebar.recipe.FluidOrItem
import io.github.pylonmc.rebar.recipe.RecipeInput
import io.github.pylonmc.rebar.util.gui.GuiItems
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.SmithingRecipe
import org.bukkit.inventory.SmithingTransformRecipe
import org.bukkit.inventory.SmithingTrimRecipe
import xyz.xenondevs.invui.gui.Gui


sealed class SmithingRecipeWrapper(recipe: SmithingRecipe) : VanillaRecipeWrapper {

    abstract override val recipe: SmithingRecipe
    override val inputs: List<RecipeInput> = listOf(recipe.base.asRecipeInput(), recipe.addition.asRecipeInput())
    override val results: List<FluidOrItem> = listOf(FluidOrItem.of(recipe.result))

    override fun display() = Gui.builder()
        .setStructure(
            "# # # # # # # # #",
            "# # # # # # # # #",
            "# b # 0 1 2 # r #",
            "# # # # # # # # #",
            "# # # # # # # # #",
        )
        .addIngredient('#', GuiItems.backgroundBlack())
        .addIngredient('b', ItemButton.of(ItemStack(Material.SMITHING_TABLE)))
        .addIngredient('0', ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE))
        .addIngredient('1', ItemButton.of(recipe.base))
        .addIngredient('2', ItemButton.of(recipe.addition))
        .addIngredient('r', ItemButton.of(recipe.result))
        .build()

    override fun getKey(): NamespacedKey = recipe.key
}

class SmithingTransformRecipeWrapper(override val recipe: SmithingTransformRecipe) : SmithingRecipeWrapper(recipe)
class SmithingTrimRecipeWrapper(override val recipe: SmithingTrimRecipe) : SmithingRecipeWrapper(recipe)

/**
 * Key: `minecraft:smithing_transform`
 */
object SmithingTransformRecipeType : VanillaRecipeType<SmithingTransformRecipeWrapper>("smithing_transform") {

    fun addRecipe(recipe: SmithingTransformRecipe) = super.addRecipe(SmithingTransformRecipeWrapper(recipe))

    override fun loadRecipe(key: NamespacedKey, section: ConfigSection): SmithingTransformRecipeWrapper {
        val template = section.getOrThrow("template", ConfigAdapter.RECIPE_INPUT_ITEM)
        val base = section.getOrThrow("base", ConfigAdapter.RECIPE_INPUT_ITEM)
        val addition = section.getOrThrow("addition", ConfigAdapter.RECIPE_INPUT_ITEM)
        val result = section.getOrThrow("result", ConfigAdapter.ITEM_STACK)
        return SmithingTransformRecipeWrapper(
            SmithingTransformRecipe(
                key,
                result,
                template.asRecipeChoice(),
                base.asRecipeChoice(),
                addition.asRecipeChoice()
            )
        )
    }
}

/**
 * Key: `minecraft:smithing_trim`
 */
object SmithingTrimRecipeType : VanillaRecipeType<SmithingTrimRecipeWrapper>("smithing_trim") {

    private val TRIM_PATTERN_ADAPTER = ConfigAdapter.KEYED.fromRegistry(
        RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_PATTERN)
    )

    fun addRecipe(recipe: SmithingTrimRecipe) = super.addRecipe(SmithingTrimRecipeWrapper(recipe))

    override fun loadRecipe(key: NamespacedKey, section: ConfigSection): SmithingTrimRecipeWrapper {
        val pattern = section.getOrThrow("pattern", TRIM_PATTERN_ADAPTER)
        val template = section.getOrThrow("template", ConfigAdapter.RECIPE_INPUT_ITEM)
        val base = section.getOrThrow("base", ConfigAdapter.RECIPE_INPUT_ITEM)
        val addition = section.getOrThrow("addition", ConfigAdapter.RECIPE_INPUT_ITEM)
        return SmithingTrimRecipeWrapper(
            SmithingTrimRecipe(
                key,
                template.asRecipeChoice(),
                base.asRecipeChoice(),
                addition.asRecipeChoice(),
                pattern
            )
        )
    }
}
