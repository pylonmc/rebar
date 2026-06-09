package io.github.pylonmc.rebar.recipe.vanilla

import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import io.github.pylonmc.rebar.guide.button.ItemButton
import io.github.pylonmc.rebar.recipe.FluidOrItem
import io.github.pylonmc.rebar.recipe.FluidOrItemChoice
import io.github.pylonmc.rebar.recipe.ItemChoice
import io.github.pylonmc.rebar.recipe.RebarRecipe
import io.github.pylonmc.rebar.util.gui.GuiItems
import io.github.pylonmc.rebar.util.isSymmetrical
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.*
import org.bukkit.inventory.recipe.CraftingBookCategory
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.item.Item
import kotlin.math.max
import kotlin.math.min

data class CraftingInput(
    val stacks: List<ItemStack?>,
    val width: Int,
    val height: Int,
    val horizontalSpace: Int,
    val verticalSpace: Int,
    val ingredientCount: Int = stacks.count { it != null && !it.isEmpty }
) {
    fun getItem(x: Int, y: Int) = stacks[x + y * this.width]
}

data class CraftingRecipeShape private constructor(
    val key: Map<Char, ItemChoice>,
    val pattern: List<String>,
    val ingredients: List<ItemChoice?>,
    val width: Int,
    val height: Int,
    val ingredientCount: Int = ingredients.count { it != null },
    val symmetrical: Boolean = isSymmetrical(width, height, ingredients)
) {

    fun getIngredient(x: Int, y: Int) = ingredients[x + y * width]

    fun getFlippedIngredient(x: Int, y: Int) = ingredients[width - x - 1 + y * width]

    fun matches(input: CraftingInput): Boolean {
        if (ingredientCount != input.ingredientCount || width != input.width || height != input.height) {
            return false
        } else if (matches(input, false)) {
            return true
        }
        return !symmetrical && matches(input, true)
    }

    private fun matches(input: CraftingInput, xFlip: Boolean): Boolean {
        for (y in 0 until height) {
            for (x in 0 until width) {
                val expected = if(xFlip) {
                    getFlippedIngredient(x, y)
                } else {
                    getIngredient(x, y)
                }

                val actual = input.getItem(x, y)
                if (!ItemChoice.validate(actual, expected)) {
                    return false
                }
            }
        }
        return true
    }

    companion object {
        fun of(key: Map<Char, ItemChoice>, pattern: List<String>): CraftingRecipeShape {
            val shrunkPattern = shrinkPattern(pattern)
            val ingredients = prepareIngredients(key, shrunkPattern)
            val width = shrunkPattern[0].length
            val height = shrunkPattern.size
            return CraftingRecipeShape(key, shrunkPattern, ingredients, width, height)
        }

        fun shrinkPattern(pattern: List<String>): List<String> {
            var left = Integer.MAX_VALUE
            var right = 0
            var top = 0
            var bottom = 0
            
            for ((row, line) in pattern.withIndex()) {
                val lastIngredient = line.indexOfLast { it != ' ' }
                left = min(left, line.indexOfFirst { it != ' ' })
                right = max(right, lastIngredient)
                if (lastIngredient < 0) {
                    if (top == row) top++
                    bottom++
                } else {
                    bottom = 0
                }
            }

            if (pattern.size != bottom) {
                val rightEdge = right + 1
                val rows = pattern.size - bottom - top
                val result = mutableListOf<String>()
                for (line in 0 until rows) {
                    result.add(pattern[line + top].substring(left, rightEdge))
                }
                return result
            }
            return emptyList()
        }

        fun prepareIngredients(key: Map<Char, ItemChoice>, pattern: List<String>): List<ItemChoice?> {
            if (key.keys.any { char -> pattern.none { it.contains(char) } }) {
                throw IllegalArgumentException("Recipe key defines characters not used in the pattern")
            }

            val ingredients = mutableListOf<ItemChoice?>()
            for (line in pattern) {
                for (char in line) {
                    ingredients.add(if (char == ' ') null else key[char] ?: throw IllegalArgumentException("Recipe pattern defines character $char not present in the key!"))
                }
            }
            return ingredients
        }
    }
}

sealed class AbstractCraftingRebarRecipe(
    val category: CraftingBookCategory,
    val group: String?,
    val key: NamespacedKey
) : RebarRecipe {
    override fun getKey() = this.key
}

class ShapedRebarRecipe(
    val shape: CraftingRecipeShape,
    val result: FluidOrItem.Item,
    category: CraftingBookCategory,
    group: String?,
    key: NamespacedKey
) : AbstractCraftingRebarRecipe(category, group, key) {
    override val inputs = shape.ingredients.filterNotNull()
    override val results = listOf(result)

    override fun display(): Gui {
        val gui = Gui.builder()
            .setStructure(
                "# # # # # # # # #",
                "# # # 0 1 2 # # #",
                "# b # 3 4 5 # r #",
                "# # # 6 7 8 # # #",
                "# # # # # # # # #",
            )
            .addIngredient('#', GuiItems.backgroundBlack())
            .addIngredient('b', ItemButton.of(ItemStack.of(Material.CRAFTING_TABLE)))
            .addIngredient('r', ItemButton.of(result))
            .build()

        for (x in 0 until shape.width) {
            for (y in 0 until shape.height) {
                gui.setItem(12 + x + 9 * y, ItemButton.of(shape.getIngredient(x, y)))
            }
        }
        return gui
    }
}

sealed class AbstractShapelessRebarRecipe(
    category: CraftingBookCategory,
    group: String?,
    key: NamespacedKey
) : AbstractCraftingRebarRecipe(category, group, key) {
    override fun display() = Gui.builder()
        .setStructure(
            "# # # # # # # # #",
            "# # # 0 1 2 # # #",
            "# b # 3 4 5 # r #",
            "# # # 6 7 8 # # #",
            "# # # # # # # # #",
        )
        .addIngredient('#', GuiItems.backgroundBlack())
        .addIngredient('b', ItemButton.of(ItemStack.of(Material.CRAFTING_TABLE)))
        .addIngredient('0', inputs.getOrNull(0)?.button() ?: Item.EMPTY)
        .addIngredient('1', inputs.getOrNull(0)?.button() ?: Item.EMPTY)
        .addIngredient('2', inputs.getOrNull(0)?.button() ?: Item.EMPTY)
        .addIngredient('3', inputs.getOrNull(0)?.button() ?: Item.EMPTY)
        .addIngredient('4', inputs.getOrNull(0)?.button() ?: Item.EMPTY)
        .addIngredient('5', inputs.getOrNull(0)?.button() ?: Item.EMPTY)
        .addIngredient('6', inputs.getOrNull(0)?.button() ?: Item.EMPTY)
        .addIngredient('7', inputs.getOrNull(0)?.button() ?: Item.EMPTY)
        .addIngredient('8', inputs.getOrNull(0)?.button() ?: Item.EMPTY)
        .addIngredient('r', results.getOrNull(0)?.button() ?: Item.EMPTY)
        .build()
}

class ShapelessRebarRecipe(
    val ingredients: List<ItemChoice>,

    category: CraftingBookCategory,
    group: String?,
    key: NamespacedKey
) : AbstractShapelessRebarRecipe(category, group, key) {

}

class TransmuteRebarRecipe(
    category: CraftingBookCategory,
    group: String?,
    key: NamespacedKey
) : AbstractShapelessRebarRecipe(category, group, key) {

}

class ShapelessRecipeWrapper(override val recipe: ShapelessRecipe) : AShapelessRecipeWrapper(recipe) {
    override val choiceList = recipe.choiceList
}

class TransmuteRecipeWrapper(override val recipe: TransmuteRecipe) : AShapelessRecipeWrapper(recipe) {
    override val choiceList = listOf(recipe.input, recipe.material)
}

private val CRAFTING_BOOK_CATEGORY_ADAPTER = ConfigAdapter.ENUM.from<CraftingBookCategory>()

/**
 * Key: `minecraft:crafting_shaped`
 */
object ShapedRecipeType : VanillaRecipeType<ShapedRebarRecipe>("crafting_shaped") {

    fun addRecipe(recipe: ShapedRecipe) = super.addRecipe(ShapedRebarRecipe(recipe))

    override fun loadRecipe(key: NamespacedKey, section: ConfigSection): ShapedRebarRecipe {
        val ingredientKey = section.getOrThrow("key", ConfigAdapter.MAP.from(ConfigAdapter.CHAR, ConfigAdapter.RECIPE_CHOICE))
        val pattern = section.getOrThrow("pattern", ConfigAdapter.LIST.from(ConfigAdapter.STRING))
        val result = section.getOrThrow("result", ConfigAdapter.ITEM_STACK)

        val recipe = ShapedRecipe(key, result)
        recipe.shape(*pattern.toTypedArray())
        for ((character, item) in ingredientKey) {
            recipe.setIngredient(character, item)
        }
        section.get("category", CRAFTING_BOOK_CATEGORY_ADAPTER)?.let { recipe.category = it }
        section.get("group", ConfigAdapter.STRING)?.let { recipe.group = it }
        return ShapedRebarRecipe(recipe)
    }
}

/**
 * Key: `minecraft:crafting_shapeless`
 */
object ShapelessRecipeType : VanillaRecipeType<ShapelessRecipeWrapper>("crafting_shapeless") {

    fun addRecipe(recipe: ShapelessRecipe) = super.addRecipe(ShapelessRecipeWrapper(recipe))

    override fun loadRecipe(key: NamespacedKey, section: ConfigSection): ShapelessRecipeWrapper {
        val ingredients = section.getOrThrow("ingredients", ConfigAdapter.LIST.from(ConfigAdapter.RECIPE_CHOICE))
        val result = section.getOrThrow("result", ConfigAdapter.ITEM_STACK)

        val recipe = ShapelessRecipe(key, result)
        for (ingredient in ingredients) {
            recipe.addIngredient(ingredient)
        }
        section.get("category", CRAFTING_BOOK_CATEGORY_ADAPTER)?.let { recipe.category = it }
        section.get("group", ConfigAdapter.STRING)?.let { recipe.group = it }
        return ShapelessRecipeWrapper(recipe)
    }
}

/**
 * Key: `minecraft:crafting_transmute`
 */
object TransmuteRecipeType : VanillaRecipeType<TransmuteRecipeWrapper>("crafting_transmute") {

    fun addRecipe(recipe: TransmuteRecipe) = super.addRecipe(TransmuteRecipeWrapper(recipe))

    override fun loadRecipe(key: NamespacedKey, section: ConfigSection): TransmuteRecipeWrapper {
        val input = section.getOrThrow("input", ConfigAdapter.RECIPE_CHOICE)
        val material = section.getOrThrow("material", ConfigAdapter.RECIPE_CHOICE)
        val result = section.getOrThrow("result", ConfigAdapter.MATERIAL)
        val category = section.get("category", CRAFTING_BOOK_CATEGORY_ADAPTER, CraftingBookCategory.MISC)
        val group = section.get("group", ConfigAdapter.STRING, "")
        val recipe = TransmuteRecipe(key, result, input, material)
        recipe.category = category
        recipe.group = group
        return TransmuteRecipeWrapper(recipe)
    }
}