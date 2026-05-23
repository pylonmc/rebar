package io.github.pylonmc.rebar.recipe.vanilla

import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import io.github.pylonmc.rebar.guide.button.ItemButton
import io.github.pylonmc.rebar.i18n.RebarArgument
import io.github.pylonmc.rebar.item.base.VanillaCookingItem
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.recipe.FluidOrItem
import io.github.pylonmc.rebar.recipe.RecipeInput
import io.github.pylonmc.rebar.util.gui.GuiItems
import io.github.pylonmc.rebar.util.gui.unit.UnitFormat
import io.github.pylonmc.rebar.util.isRebarAndIsNot
import io.papermc.paper.datacomponent.DataComponentTypes
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.*
import org.bukkit.inventory.recipe.CookingBookCategory
import xyz.xenondevs.invui.gui.Gui

sealed class CookingRecipeWrapper(final override val recipe: CookingRecipe<*>, val recipeInput: RecipeInput.Item) : VanillaRecipeWrapper {
    override val inputs: List<RecipeInput> = listOf(recipeInput)
    override val results: List<FluidOrItem> = listOf(FluidOrItem.of(recipe.result))
    override fun getKey(): NamespacedKey = recipe.key
    fun matches(item: ItemStack) =
        if (item.isRebarAndIsNot<VanillaCookingItem>() || key !in VanillaRecipeType.nonRebarRecipes) item in recipeInput
        else recipe.inputChoice.test(item)

    protected abstract val displayBlock: Material

    override fun display(): Gui = Gui.builder()
        .setStructure(
            "# # # # # # # # #",
            "# # # # # # # # #",
            "# b # # i f o # #",
            "# # # # # # # # #",
            "# # # # # # # # #",
        )
        .addIngredient('#', GuiItems.backgroundBlack())
        .addIngredient('b', ItemStack(displayBlock))
        .addIngredient('i', ItemButton.of(recipe.inputChoice))
        .addIngredient(
            'f', GuiItems.progressCyclingItem(
                recipe.cookingTime,
                ItemStackBuilder.of(Material.COAL)
                    .name(
                        Component.translatable(
                            "rebar.guide.recipe.cooking",
                            RebarArgument.of("time", UnitFormat.SECONDS.format(recipe.cookingTime / 20))
                        )
                    )
            )
        )
        .addIngredient('o', ItemButton.of(recipe.result))
        .build()
}

class BlastingRecipeWrapper @JvmOverloads constructor(
    recipe: BlastingRecipe,
    recipeInput: RecipeInput.Item = recipe.inputChoice.asRecipeInput()
) : CookingRecipeWrapper(recipe, recipeInput) {
    override val displayBlock = Material.BLAST_FURNACE
}

class CampfireRecipeWrapper @JvmOverloads constructor(
    recipe: CampfireRecipe,
    recipeInput: RecipeInput.Item = recipe.inputChoice.asRecipeInput()
) : CookingRecipeWrapper(recipe, recipeInput) {
    override val displayBlock = Material.CAMPFIRE
}

class FurnaceRecipeWrapper @JvmOverloads constructor(
    recipe: FurnaceRecipe,
    recipeInput: RecipeInput.Item = recipe.inputChoice.asRecipeInput()
) : CookingRecipeWrapper(recipe, recipeInput) {
    override val displayBlock = Material.FURNACE
}

class SmokingRecipeWrapper @JvmOverloads constructor(
    recipe: SmokingRecipe,
    recipeInput: RecipeInput.Item = recipe.inputChoice.asRecipeInput()
) : CookingRecipeWrapper(recipe, recipeInput) {
    override val displayBlock = Material.SMOKER
}

@Suppress("UnstableApiUsage")
private inline fun <T : CookingRecipe<T>, U : CookingRecipeWrapper> loadCookingRecipe(
    key: NamespacedKey,
    config: ConfigSection,
    defaultCookingTime: Int,
    recipeCons: (NamespacedKey, ItemStack, RecipeChoice, Float, Int) -> T,
    wrapperCons: (T, RecipeInput.Item) -> U
): U {
    val cookingTime = config.get("cookingtime", ConfigAdapter.INTEGER, defaultCookingTime)
    val experience = config.get("experience", ConfigAdapter.FLOAT, 0f)
    val ingredient = config.getOrThrow("ingredient", ConfigAdapter.RECIPE_INPUT_ITEM)
    if (ingredient.representativeItems.any{ it.hasData(DataComponentTypes.MAX_DAMAGE) }) {
        ingredient.ignoreComponents.add(DataComponentTypes.DAMAGE)
    }
    val result = config.getOrThrow("result", ConfigAdapter.ITEM_STACK)
    val recipe = recipeCons(key, result, ingredient.asRecipeChoice(), experience, cookingTime)
    config.get("category", ConfigAdapter.ENUM.from<CookingBookCategory>())?.let { recipe.category = it }
    config.get("group", ConfigAdapter.STRING)?.let { recipe.group = it }
    return wrapperCons(recipe, ingredient)
}

/**
 * Key: `minecraft:blasting`
 */
object BlastingRecipeType : VanillaRecipeType<BlastingRecipeWrapper>("blasting") {

    @JvmOverloads
    fun addRecipe(recipe: BlastingRecipe, recipeInput: RecipeInput.Item = recipe.inputChoice.asRecipeInput()) =
        super.addRecipe(BlastingRecipeWrapper(recipe, recipeInput))

    override fun loadRecipe(key: NamespacedKey, section: ConfigSection) =
        loadCookingRecipe(key, section, 100, ::BlastingRecipe, ::BlastingRecipeWrapper)
}

/**
 * Key: `minecraft:campfire_cooking`
 *
 * Despite the vanilla default cooking time being 100 ticks, we set it to 600 ticks here
 * to match the actual in-game behavior
 */
object CampfireRecipeType : VanillaRecipeType<CampfireRecipeWrapper>("campfire_cooking") {

    @JvmOverloads
    fun addRecipe(recipe: CampfireRecipe, recipeInput: RecipeInput.Item = recipe.inputChoice.asRecipeInput()) =
        super.addRecipe(CampfireRecipeWrapper(recipe, recipeInput))

    override fun loadRecipe(key: NamespacedKey, section: ConfigSection) =
        loadCookingRecipe(key, section, 600, ::CampfireRecipe, ::CampfireRecipeWrapper)
}

/**
 * Key: `minecraft:smelting`
 */
object FurnaceRecipeType : VanillaRecipeType<FurnaceRecipeWrapper>("smelting") {

    @JvmOverloads
    fun addRecipe(recipe: FurnaceRecipe, recipeInput: RecipeInput.Item = recipe.inputChoice.asRecipeInput()) =
        super.addRecipe(FurnaceRecipeWrapper(recipe, recipeInput))

    override fun loadRecipe(key: NamespacedKey, section: ConfigSection) =
        loadCookingRecipe(key, section, 200, ::FurnaceRecipe, ::FurnaceRecipeWrapper)
}

/**
 * Key: `minecraft:smoking`
 */
object SmokingRecipeType : VanillaRecipeType<SmokingRecipeWrapper>("smoking") {

    @JvmOverloads
    fun addRecipe(recipe: SmokingRecipe, recipeInput: RecipeInput.Item = recipe.inputChoice.asRecipeInput()) =
        super.addRecipe(SmokingRecipeWrapper(recipe, recipeInput))

    override fun loadRecipe(key: NamespacedKey, section: ConfigSection) =
        loadCookingRecipe(key, section, 100, ::SmokingRecipe, ::SmokingRecipeWrapper)
}
