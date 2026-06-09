package io.github.pylonmc.rebar.recipe.vanilla

import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import io.github.pylonmc.rebar.guide.button.ItemButton
import io.github.pylonmc.rebar.i18n.RebarArgument
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.recipe.FluidOrItem
import io.github.pylonmc.rebar.recipe.ItemChoice
import io.github.pylonmc.rebar.util.gui.GuiItems
import io.github.pylonmc.rebar.util.gui.unit.UnitFormat
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.*
import org.bukkit.inventory.recipe.CookingBookCategory
import xyz.xenondevs.invui.gui.Gui


sealed class CookingRebarRecipe(
    val ingredient: ItemChoice,
    val result: FluidOrItem.Item,
    val experience: Float,
    val cookingTime: Int,
    val category: CookingBookCategory,
    val group: String,
    @JvmField val key: NamespacedKey
) : VanillaRebarRecipe {
    override val inputs = listOf(ingredient)
    override val results = listOf(result)

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
        .addIngredient('b', ItemStack.of(displayBlock))
        .addIngredient('i', ItemButton.of(ingredient))
        .addIngredient(
            'f', GuiItems.progressCyclingItem(
                cookingTime,
                ItemStackBuilder.of(Material.COAL)
                    .name(
                        Component.translatable(
                            "rebar.guide.recipe.cooking",
                            RebarArgument.of("time", UnitFormat.SECONDS.format(cookingTime / 20))
                        )
                    )
            )
        )
        .addIngredient('o', ItemButton.of(result))
        .build()

    override fun getKey(): NamespacedKey = key
}

class BlastingRebarRecipe(
    ingredient: ItemChoice,
    result: FluidOrItem.Item,
    experience: Float,
    cookingTime: Int,
    category: CookingBookCategory,
    group: String,
    key: NamespacedKey,
    override val recipe: BlastingRecipe = BlastingRecipe(
        key, result.item, ingredient.toRepresentativeRecipeChoice(),
        experience, cookingTime
    ).apply {
        this.category = category
        this.group = group
    }
) : CookingRebarRecipe(ingredient, result, experience, cookingTime, category, group, key) {
    override val displayBlock = Material.BLAST_FURNACE

    companion object {
        fun fromVanilla(recipe: BlastingRecipe): BlastingRebarRecipe {
            return BlastingRebarRecipe(
                recipe.inputChoice.toItemChoice(),
                FluidOrItem.of(recipe.result),
                recipe.experience,
                recipe.cookingTime,
                recipe.category,
                recipe.group,
                recipe.key,
                recipe
            )
        }
    }
}

class CampfireRebarRecipe(
    ingredient: ItemChoice,
    result: FluidOrItem.Item,
    experience: Float,
    cookingTime: Int,
    category: CookingBookCategory,
    group: String,
    key: NamespacedKey,
    override val recipe: CampfireRecipe = CampfireRecipe(
        key, result.item, ingredient.toRepresentativeRecipeChoice(),
        experience, cookingTime
    ).apply {
        this.category = category
        this.group = group
    }
) : CookingRebarRecipe(ingredient, result, experience, cookingTime, category, group, key) {
    override val displayBlock = Material.CAMPFIRE

    companion object {
        fun fromVanilla(recipe: CampfireRecipe): CampfireRebarRecipe {
            return CampfireRebarRecipe(
                recipe.inputChoice.toItemChoice(),
                FluidOrItem.of(recipe.result),
                recipe.experience,
                recipe.cookingTime,
                recipe.category,
                recipe.group,
                recipe.key,
                recipe
            )
        }
    }
}

class FurnaceRebarRecipe(
    ingredient: ItemChoice,
    result: FluidOrItem.Item,
    experience: Float,
    cookingTime: Int,
    category: CookingBookCategory,
    group: String,
    key: NamespacedKey,
    override val recipe: FurnaceRecipe = FurnaceRecipe(
        key, result.item, ingredient.toRepresentativeRecipeChoice(),
        experience, cookingTime
    ).apply {
        this.category = category
        this.group = group
    }
) : CookingRebarRecipe(ingredient, result, experience, cookingTime, category, group, key) {
    override val displayBlock = Material.FURNACE

    companion object {
        fun fromVanilla(recipe: FurnaceRecipe): FurnaceRebarRecipe {
            return FurnaceRebarRecipe(
                recipe.inputChoice.toItemChoice(),
                FluidOrItem.of(recipe.result),
                recipe.experience,
                recipe.cookingTime,
                recipe.category,
                recipe.group,
                recipe.key,
                recipe
            )
        }
    }
}

class SmokingRebarRecipe(
    ingredient: ItemChoice,
    result: FluidOrItem.Item,
    experience: Float,
    cookingTime: Int,
    category: CookingBookCategory,
    group: String,
    key: NamespacedKey,
    override val recipe: SmokingRecipe = SmokingRecipe(
        key, result.item, ingredient.toRepresentativeRecipeChoice(),
        experience, cookingTime
    ).apply {
        this.category = category
        this.group = group
    }
) : CookingRebarRecipe(ingredient, result, experience, cookingTime, category, group, key) {
    override val displayBlock = Material.SMOKER

    companion object {
        fun fromVanilla(recipe: SmokingRecipe): SmokingRebarRecipe {
            return SmokingRebarRecipe(
                recipe.inputChoice.toItemChoice(),
                FluidOrItem.of(recipe.result),
                recipe.experience,
                recipe.cookingTime,
                recipe.category,
                recipe.group,
                recipe.key,
                recipe
            )
        }
    }
}

private val COOKING_BOOK_CATEGORY_ADAPTER = ConfigAdapter.ENUM.from<CookingBookCategory>()

private inline fun <T : CookingRebarRecipe> loadCookingRecipe(
    key: NamespacedKey,
    config: ConfigSection,
    defaultCookingTime: Int,
    cons: (ItemChoice, FluidOrItem.Item, Float, Int, CookingBookCategory, String, NamespacedKey) -> T
): T {
    val ingredient = config.getOrThrow("ingredient", ConfigAdapter.ITEM_CHOICE)
    val result = FluidOrItem.of(config.getOrThrow("result", ConfigAdapter.ITEM_STACK))
    val experience = config.get("experience", ConfigAdapter.FLOAT, 0f)
    val cookingTime = config.get("cookingtime", ConfigAdapter.INTEGER, defaultCookingTime)
    val category = config.get("category", COOKING_BOOK_CATEGORY_ADAPTER, CookingBookCategory.MISC)
    val group = config.get("group", ConfigAdapter.STRING, "")
    return cons(ingredient, result, experience, cookingTime, category, group, key)
}

/**
 * Key: `minecraft:blasting`
 */
object BlastingRecipeType : VanillaRecipeType<BlastingRebarRecipe>("blasting") {
    override fun loadRecipe(key: NamespacedKey, section: ConfigSection) =
        loadCookingRecipe(key, section, 100, ::BlastingRebarRecipe)
}

/**
 * Key: `minecraft:campfire_cooking`
 *
 * Despite the vanilla default cooking time being 100 ticks, we set it to 600 ticks here
 * to match the actual in-game behavior
 */
object CampfireRecipeType : VanillaRecipeType<CampfireRebarRecipe>("campfire_cooking") {
    override fun loadRecipe(key: NamespacedKey, section: ConfigSection) =
        loadCookingRecipe(key, section, 600, ::CampfireRebarRecipe)
}

/**
 * Key: `minecraft:smelting`
 */
object FurnaceRecipeType : VanillaRecipeType<FurnaceRebarRecipe>("smelting") {
    override fun loadRecipe(key: NamespacedKey, section: ConfigSection) =
        loadCookingRecipe(key, section, 200, ::FurnaceRebarRecipe)
}

/**
 * Key: `minecraft:smoking`
 */
object SmokingRecipeType : VanillaRecipeType<SmokingRebarRecipe>("smoking") {
    override fun loadRecipe(key: NamespacedKey, section: ConfigSection) =
        loadCookingRecipe(key, section, 100, ::SmokingRebarRecipe)
}
