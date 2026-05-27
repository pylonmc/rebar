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

private fun getCookingTime(config: ConfigSection, defaultCookingTime: Int): Int {
    return config.get("cookingtime", ConfigAdapter.INTEGER, defaultCookingTime)
}
private fun getExperience(config: ConfigSection): Float {
    return config.get("experience", ConfigAdapter.FLOAT, 0f)
}
@Suppress("UnstableApiUsage")
private fun getIngredient(config: ConfigSection): RecipeInput.Item {
    val ingredient = config.getOrThrow("ingredient", ConfigAdapter.RECIPE_INPUT_ITEM)
    if (ingredient.representativeItems.any{ it.hasData(DataComponentTypes.MAX_DAMAGE) }) {
        ingredient.ignoreComponents.add(DataComponentTypes.DAMAGE)
    }
    return ingredient
}
private fun getResult(config: ConfigSection): ItemStack {
    return config.getOrThrow("result", ConfigAdapter.ITEM_STACK)
}
private fun getCategory(config: ConfigSection): CookingBookCategory? {
    return config.get("category", ConfigAdapter.ENUM.from<CookingBookCategory>())
}
private fun getGroup(config: ConfigSection): String? {
    return config.get("group", ConfigAdapter.STRING)
}

/**
 * Key: `minecraft:blasting`
 */
object BlastingRecipeType : VanillaRecipeType<BlastingRecipeWrapper>("blasting") {

    @JvmOverloads
    fun addRecipe(recipe: BlastingRecipe, recipeInput: RecipeInput.Item = recipe.inputChoice.asRecipeInput()) =
        super.addRecipe(BlastingRecipeWrapper(recipe, recipeInput))

    override fun loadRecipe(key: NamespacedKey, section: ConfigSection): BlastingRecipeWrapper {
        val cookingTime = getCookingTime(section, 100)
        val experience = getExperience(section)
        val ingredient = getIngredient(section)
        val result = getResult(section)
        val recipe = BlastingRecipe(key, result, ingredient.asRecipeChoice(), experience, cookingTime)
        getCategory(section)?.let { recipe.category = it }
        getGroup(section)?.let { recipe.group = it }
        return BlastingRecipeWrapper(recipe, ingredient)
    }
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


    override fun loadRecipe(key: NamespacedKey, section: ConfigSection): CampfireRecipeWrapper {
        val cookingTime = getCookingTime(section, 600)
        val experience = getExperience(section)
        val ingredient = getIngredient(section)
        val result = getResult(section)
        val recipe = CampfireRecipe(key, result, ingredient.asRecipeChoice(), experience, cookingTime)
        getCategory(section)?.let { recipe.category = it }
        getGroup(section)?.let { recipe.group = it }
        return CampfireRecipeWrapper(recipe, ingredient)
    }
}

/**
 * Key: `minecraft:smelting`
 */
object FurnaceRecipeType : VanillaRecipeType<FurnaceRecipeWrapper>("smelting") {

    @JvmOverloads
    fun addRecipe(recipe: FurnaceRecipe, recipeInput: RecipeInput.Item = recipe.inputChoice.asRecipeInput()) =
        super.addRecipe(FurnaceRecipeWrapper(recipe, recipeInput))

    override fun loadRecipe(key: NamespacedKey, section: ConfigSection) : FurnaceRecipeWrapper {
        val cookingTime = getCookingTime(section, 200)
        val experience = getExperience(section)
        val ingredient = getIngredient(section)
        val result = getResult(section)
        val recipe = FurnaceRecipe(key, result, ingredient.asRecipeChoice(), experience, cookingTime)
        getCategory(section)?.let { recipe.category = it }
        getGroup(section)?.let { recipe.group = it }
        return FurnaceRecipeWrapper(recipe, ingredient)
    }
}

/**
 * Key: `minecraft:smoking`
 */
object SmokingRecipeType : VanillaRecipeType<SmokingRecipeWrapper>("smoking") {

    @JvmOverloads
    fun addRecipe(recipe: SmokingRecipe, recipeInput: RecipeInput.Item = recipe.inputChoice.asRecipeInput()) =
        super.addRecipe(SmokingRecipeWrapper(recipe, recipeInput))

    override fun loadRecipe(key: NamespacedKey, section: ConfigSection) : SmokingRecipeWrapper {
        val cookingTime = getCookingTime(section, 100)
        val experience = getExperience(section)
        val ingredient = getIngredient(section)
        val result = getResult(section)
        val recipe = SmokingRecipe(key, result, ingredient.asRecipeChoice(), experience, cookingTime)
        getCategory(section)?.let { recipe.category = it }
        getGroup(section)?.let { recipe.group = it }
        return SmokingRecipeWrapper(recipe, ingredient)
    }
}