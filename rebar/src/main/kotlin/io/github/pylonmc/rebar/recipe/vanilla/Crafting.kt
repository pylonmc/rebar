package io.github.pylonmc.rebar.recipe.vanilla

import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import io.github.pylonmc.rebar.guide.button.ItemButton
import io.github.pylonmc.rebar.recipe.FluidOrItem
import io.github.pylonmc.rebar.recipe.ItemChoice
import io.github.pylonmc.rebar.util.gui.GuiItems
import io.github.pylonmc.rebar.util.isSymmetrical
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.*
import org.bukkit.inventory.recipe.CraftingBookCategory
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.item.Item
import kotlin.collections.iterator
import kotlin.math.max
import kotlin.math.min

data class CraftingInput(
    val stacks: List<ItemStack?>,
    val width: Int,
    val height: Int,
    val ingredientCount: Int = stacks.count { it != null && !it.isEmpty }
) {
    fun getItem(x: Int, y: Int) = stacks[x + y * this.width]

    companion object {
        val EMPTY = CraftingInput(emptyList(), 0, 0, 0)

        fun of(inventory: CrafterInventory): CraftingInput {
            return of(3, 3, inventory.contents.toList())
        }

        fun of(inventory: CraftingInventory): CraftingInput {
            return of(3, 3, inventory.contents.toList())
        }

        fun of(width: Int, height: Int, items: List<ItemStack?>): CraftingInput {
            if (width == 0 || height == 0) return EMPTY

            var left = width -1
            var right = 0
            var top = height - 1
            var bottom = 0

            for (y in 0 until height) {
                var rowEmpty = true
                for (x in 0 until width) {
                    val item = items[x + y * width]
                    if (item != null && !item.isEmpty) {
                        left = min(left, x)
                        right = max(right, x)
                        rowEmpty = false
                    }
                }

                if (!rowEmpty) {
                    top = min(top, y)
                    bottom = max(bottom, y)
                }
            }

            val newWidth = right - left + 1
            val newHeight = bottom - top + 1
            if (newWidth <= 0 || newHeight <= 0) {
                return EMPTY
            } else if (newWidth == width && newHeight == height) {
                return CraftingInput(items, width, height)
            }

            val newItems = mutableListOf<ItemStack?>()
            for (y in 0 until newHeight) {
                for (x in 0 until newWidth) {
                    newItems.add(items[x + left + (y + top) * width])
                }
            }
            return CraftingInput(newItems, newWidth, newHeight)
        }
    }
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
    val result: FluidOrItem.Item,
    val category: CraftingBookCategory,
    val group: String,
    @JvmField val key: NamespacedKey
) : VanillaRebarRecipe {
    abstract fun matches(input: CraftingInput): Boolean
    override fun getKey() = this.key
}

class ShapedRebarRecipe(
    val shape: CraftingRecipeShape,
    result: FluidOrItem.Item,
    category: CraftingBookCategory,
    group: String,
    key: NamespacedKey,
    override val recipe: ShapedRecipe = ShapedRecipe(key, result.item).apply {
        this.category = category
        this.group = group
        this.shape(*shape.pattern.toTypedArray())
        for (ingredient in shape.key) {
            this.setIngredient(ingredient.key, ingredient.value.toRepresentativeRecipeChoice())
        }
    }
) : AbstractCraftingRebarRecipe(result, category, group, key) {
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

    override fun matches(input: CraftingInput) = shape.matches(input)

    companion object {
        fun fromVanilla(recipe: ShapedRecipe): ShapedRebarRecipe {
            return ShapedRebarRecipe(
                CraftingRecipeShape.of(
                    recipe.choiceMap.mapValues { entry -> entry.value.toItemChoice() },
                    recipe.shape.toList()
                ),
                FluidOrItem.of(recipe.result),
                recipe.category,
                recipe.group,
                recipe.key
            )
        }
    }
}

sealed class AbstractShapelessRebarRecipe(
    result: FluidOrItem.Item,
    category: CraftingBookCategory,
    group: String,
    key: NamespacedKey
) : AbstractCraftingRebarRecipe(result, category, group, key) {
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
    result: FluidOrItem.Item,
    category: CraftingBookCategory,
    group: String,
    key: NamespacedKey,
    override val recipe: ShapelessRecipe = ShapelessRecipe(key, result.item).apply {
        this.category = category
        this.group = group
        for (ingredient in ingredients) {
            this.addIngredient(ingredient.toRepresentativeRecipeChoice())
        }
    }
) : AbstractShapelessRebarRecipe(result, category, group, key) {
    override val inputs = ingredients
    override val results = listOf(result)

    override fun matches(input: CraftingInput): Boolean {
        if (input.ingredientCount != ingredients.size) return false
        val inputs = input.stacks.filterNotNullTo(mutableListOf())
        for (ingredient in ingredients) {
            var found = false
            for ((index, input) in inputs.withIndex()) {
                if (!ingredient.matches(input)) continue
                found = true
                inputs.removeAt(index)
                break
            }
            if (!found) return false
        }
        return inputs.isEmpty()
    }

    companion object {
        fun fromVanilla(recipe: ShapelessRecipe): ShapelessRebarRecipe {
            return ShapelessRebarRecipe(
                recipe.choiceList.map { it.toItemChoice() },
                FluidOrItem.Item(recipe.result),
                recipe.category,
                recipe.group,
                recipe.key,
                recipe
            )
        }
    }
}

class TransmuteRebarRecipe(
    val input: ItemChoice,
    val material: ItemChoice,
    val resultType: Material,
    category: CraftingBookCategory,
    group: String,
    key: NamespacedKey,
    override val recipe: TransmuteRecipe
) : AbstractShapelessRebarRecipe(FluidOrItem.Item.EMPTY, category, group, key) {
    override val inputs = listOf(input, material)
    override val results = emptyList<FluidOrItem.Item>()

    override fun matches(input: CraftingInput): Boolean {
        if (input.ingredientCount != 2) return false
        val first = input.stacks[0] ?: return false
        val second = input.stacks[1] ?: return false
        return if (this.input.matches(first)) {
            this.material.matches(second)
        } else if (this.input.matches(second)) {
             this.material.matches(first)
        } else false
    }

    companion object {
        fun fromVanilla(recipe: TransmuteRecipe): TransmuteRebarRecipe {
            return TransmuteRebarRecipe(
                recipe.input.toItemChoice(),
                recipe.material.toItemChoice(),
                recipe.result.type,
                recipe.category,
                recipe.group,
                recipe.key,
                recipe
            )
        }
    }
}

private val CRAFTING_BOOK_CATEGORY_ADAPTER = ConfigAdapter.ENUM.from<CraftingBookCategory>()

/**
 * Key: `minecraft:crafting_shaped`
 */
object ShapedRecipeType : VanillaRecipeType<ShapedRebarRecipe>("crafting_shaped") {
    override fun loadRecipe(key: NamespacedKey, section: ConfigSection): ShapedRebarRecipe {
        val ingredientKey = section.getOrThrow("key", ConfigAdapter.MAP.from(ConfigAdapter.CHAR, ConfigAdapter.ITEM_CHOICE))
        val pattern = section.getOrThrow("pattern", ConfigAdapter.LIST.from(ConfigAdapter.STRING))

        val shape = CraftingRecipeShape.of(ingredientKey, pattern)
        val result = FluidOrItem.of(section.getOrThrow("result", ConfigAdapter.ITEM_STACK))
        val category = section.get("category", CRAFTING_BOOK_CATEGORY_ADAPTER, CraftingBookCategory.MISC)
        val group = section.get("group", ConfigAdapter.STRING, "")
        return ShapedRebarRecipe(shape, result, category, group, key)
    }
}

/**
 * Key: `minecraft:crafting_shapeless`
 */
object ShapelessRecipeType : VanillaRecipeType<ShapelessRebarRecipe>("crafting_shapeless") {
    override fun loadRecipe(key: NamespacedKey, section: ConfigSection): ShapelessRebarRecipe {
        val ingredients = section.getOrThrow("ingredients", ConfigAdapter.LIST.from(ConfigAdapter.ITEM_CHOICE))
        val result = FluidOrItem.of(section.getOrThrow("result", ConfigAdapter.ITEM_STACK))
        val category = section.get("category", CRAFTING_BOOK_CATEGORY_ADAPTER, CraftingBookCategory.MISC)
        val group = section.get("group", ConfigAdapter.STRING, "")
        return ShapelessRebarRecipe(ingredients, result, category, group, key)
    }
}

/**
 * Key: `minecraft:crafting_transmute`
 */
object TransmuteRecipeType : VanillaRecipeType<TransmuteRebarRecipe>("crafting_transmute") {
    override fun loadRecipe(key: NamespacedKey, section: ConfigSection): TransmuteRebarRecipe {
        throw IllegalArgumentException("You cannot make custom transmute recipes")
    }
}