package io.github.pylonmc.rebar.recipe

import io.github.pylonmc.rebar.util.LRUCache
import io.github.pylonmc.rebar.util.hashIgnoreAmount
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import java.util.function.Predicate

object RecipeService {
    @JvmStatic
    private val cache = LRUCache<Int, RebarRecipe?>(1000)

    /**
     * Creates the cache key from the item hash and the recipe type
     */
    @JvmStatic
    private fun cacheKeyFrom(hash: Int, recipeType: RecipeType<*>) = 31 * hash + recipeType.hashCode()

    /**
     * Returns the recipe (or null if not present) cached with specified input hash and recipe type
     */
    @JvmStatic
    fun getCached(hash: Int, recipeType: RecipeType<*>) = cache[cacheKeyFrom(hash, recipeType)]

    /**
     * Returns if a recipe was cached with specified input hash and recipe type
     */
    @JvmStatic
    fun isCached(hash: Int, recipeType: RecipeType<*>) = cacheKeyFrom(hash, recipeType) in cache

    /**
     * Caches a recipe or non-existence of a recipe crafted by specified input hash and recipe type
     */
    @JvmStatic
    fun cache(hash: Int, recipeType: RecipeType<*>, recipe: RebarRecipe?) {
        cache[cacheKeyFrom(hash, recipeType)] = recipe
    }

    /**
     * Clears the recipe cache
     */
    @JvmStatic
    fun clearCache() { cache.clear() }

    /**
     * Hashes of a list of items. The order of the items affects the hash
     */
    @JvmStatic
    fun hashShapedCraftingInput2D(items: Iterable<Iterable<ItemStack?>>): Int =
        items.fold(1) { outerHash, row ->
            row.fold(outerHash) { hash, item -> 31 * hash + (item?.hashIgnoreAmount() ?: 0) }
        }

    /**
     * Hashes of a list of items. The order of the items affects the hash
     */
    @JvmStatic
    fun hashShapedCraftingInput(items: Iterable<ItemStack?>): Int =
        items.fold(1) { hash, i -> 31 * hash + (i?.hashIgnoreAmount() ?: 0) }

    /**
     * Hashes of a list of items. The order of the items does not affect the hash
     */
    @JvmStatic
    fun hashShapelessCraftingInput2D(items: Iterable<Iterable<ItemStack?>>): Int =
        items.sumOf { row -> row.sumOf { it?.hashIgnoreAmount() ?: 0 } }

    /**
     * Hashes of a list of items. The order of the items does not affect the hash
     */
    @JvmStatic
    fun hashShapelessCraftingInput(items: Iterable<ItemStack?>): Int =
        items.sumOf { it?.hashIgnoreAmount() ?: 0 }

    /**
     * Matches a shaped recipe. The items must be in the same positions, up to translation
     *
     * @param items The items the player is trying to craft with
     * @param recipeInput The inputs to the recipe
     * @param w The width of the recipe grid
     * @param h The height of the recipe grid
     */
    @JvmStatic
    fun matchesShapedRecipe(items: List<ItemStack?>, recipeInput: List<RecipeInput.Item>, w: Int, h: Int): Boolean {
        var i = -1
        var r = -1
        var iCurr: ItemStack? = null
        var rCurr: RecipeInput.Item? = null
        // Find first non-empty element of both lists
        while (i+1 < items.size && iCurr?.isEmpty ?: true) {
            i++
            iCurr = items[i]
        }
        while (r+1 < recipeInput.size && rCurr?.isEmpty() ?: true) {
            r++
            rCurr = recipeInput[r]
        }
        // Get relative vertical and horizontal offsets
        val vOffset = i/w - r/w
        val hOffset = i%w - r%w
        // The items match if either
        // - both are empty
        // - both match and are at the same relative vertical and horizontal offset as the first non-empty items
        // we want all items to match until both lists are exhausted
        while (
            (iCurr?.isEmpty ?: true && rCurr?.isEmpty() ?: true) ||
            (rCurr?.matches(iCurr) == true && i/w - r/w == vOffset && i%w - r%w == hOffset)
        ) {
            if (r >= recipeInput.size && i >= items.size) return true
            iCurr = if (i < items.size) items[i] else null
            rCurr = if (r < recipeInput.size) recipeInput[r] else null
            i++
            r++
        }
        return false
    }

    /**
     * Matches a shaped recipe, possibly mirrored along the vertical axis (e.g. vanilla crafting)
     *
     * @param items The items the player is trying to craft with
     * @param recipeInput The inputs to the recipe
     * @param w The width of the recipe grid
     * @param h The height of the recipe grid
     */
    @JvmStatic
    fun matchesShapedMirrorableRecipe(items: List<ItemStack?>, recipeInput: List<RecipeInput.Item>, w: Int, h: Int): Boolean {
        if (matchesShapedRecipe(items, recipeInput, w, h)) return true
        val mirror = items.toMutableList()
        while (mirror.size < 9) mirror.add(null)
        mirror[2] = mirror[0].also{ mirror[0] = mirror[2] }
        mirror[5] = mirror[3].also{ mirror[3] = mirror[5] }
        mirror[8] = mirror[6].also{ mirror[6] = mirror[8] }
        return matchesShapedRecipe(mirror, recipeInput, w, h)
    }

    /**
     * Matches a shapeless recipe. All items must have one matching recipe input and vice versa.
     *
     * @param items The items the player is trying to craft with
     * @param recipeInput The inputs to the recipe
     */
    @JvmStatic
    fun matchesShapelessRecipe(items: List<ItemStack?>, recipeInput: List<RecipeInput.Item>): Boolean {
        if (items.count { !(it?.isEmpty ?: true) } != recipeInput.count { !it.isEmpty() }) return false
        // Try to match each non-empty item to a recipe input
        val matchedIndices = mutableSetOf<Int>()
        outer@ for (item in items) {
            if (item?.isEmpty ?: true) continue
            for ((i, input) in recipeInput.withIndex()) {
                if (!input.isEmpty() && i !in matchedIndices && input.matches(item)) {
                    matchedIndices.add(i)
                    continue@outer
                }
            }
            // no match for item
            return false
        }
        return true
    }

    /**
     * Looks for a recipe in the given recipe type
     *
     * @param recipeType The recipe type to search
     * @param hint Key to check first, e.g. the recipe returned by MCs recipe matching
     * @param hash The recipe hash to save the result with
     * @param matches Should return true if the given recipe is the one you want
     *
     * Returns the recipe that matches, or null if none do
     */
    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <T : RebarRecipe> searchRecipes(recipeType: RecipeType<out T>, hint: NamespacedKey?, hash: Int, matches: Predicate<T>): T? {
        // Try the hint (usu. what minecraft thinks we are trying to craft)
        if (hint != null) {
            val hintRecipe = recipeType.getRecipe(hint)
            if (hintRecipe != null && matches.test(hintRecipe)) {
                cache(hash, recipeType, hintRecipe)
                return hintRecipe
            }
        }
        // Try the cache
        if (isCached(hash, recipeType)) {
            // Null in the cache means no recipe was found
            val cachedRecipe = getCached(hash, recipeType) as? T ?: return null
            if (matches.test(cachedRecipe)) return cachedRecipe
        }
        // Linear search
        for (recipe in recipeType.recipes) {
            if (matches.test(recipe)) {
                cache(hash, recipeType, recipe)
                return recipe
            }
        }
        cache(hash, recipeType, null)
        return null
    }

    /**
     * Looks for a recipe in the given recipe type
     *
     * @param recipeType The recipe type to search
     * @param hash The recipe hash to save the result with
     * @param matches Should return true if the given recipe is the one you want
     *
     * Returns the recipe that matches, or null if none do
     */
    @JvmStatic
    fun <T : RebarRecipe> searchRecipes(recipeType: RecipeType<out T>, hash: Int, matches: Predicate<T>): T?
            = searchRecipes(recipeType, null, hash, matches)
}