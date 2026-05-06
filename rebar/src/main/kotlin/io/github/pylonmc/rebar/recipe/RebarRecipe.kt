package io.github.pylonmc.rebar.recipe

import com.google.common.collect.MapMaker
import io.github.pylonmc.rebar.fluid.RebarFluid
import io.github.pylonmc.rebar.util.LRUCache
import io.github.pylonmc.rebar.util.hashIgnoreAmount
import org.bukkit.Keyed
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.gui.Gui
import java.util.function.Predicate

interface RebarRecipe : Keyed {

    val isHidden: Boolean
        get() = false

    val inputs: List<RecipeInput>
    val results: List<FluidOrItem>

    fun isInput(stack: ItemStack) = inputs.any { input ->
        when (input) {
            is RecipeInput.Item -> stack in input
            else -> false
        }
    }

    fun isInput(fluid: RebarFluid) = inputs.any { input ->
        when (input) {
            is RecipeInput.Fluid -> fluid in input.fluids
            else -> false
        }
    }

    fun isOutput(stack: ItemStack) = results.any {
        when (it) {
            is FluidOrItem.Item -> it.item.isSimilar(stack)
            else -> false
        }
    }

    fun isOutput(fluid: RebarFluid) = results.any {
        when (it) {
            is FluidOrItem.Fluid -> it.fluid == fluid
            else -> false
        }
    }

    fun display(): Gui?

    companion object {
        private val priorities = MapMaker().weakKeys().makeMap<RebarRecipe, Double>()
        private val cache = LRUCache<Int, RebarRecipe?>(1000)

        @JvmStatic
        var RebarRecipe.priority: Double
            get() = priorities.getOrDefault(this, 0.0)
            set(value) {
                priorities[this] = value
            }

        private fun cacheKey(hash: Int, recipeType: RecipeType<*>) = 31 * hash + recipeType.hashCode()

        fun getCached(hash: Int, recipeType: RecipeType<*>) = cache[cacheKey(hash, recipeType)]
        fun isCached(hash: Int, recipeType: RecipeType<*>) = cacheKey(hash, recipeType) in cache
        fun cache(hash: Int, recipeType: RecipeType<*>, recipe: RebarRecipe?) {
            cache[cacheKey(hash, recipeType)] = recipe
        }
        fun clearCache() { cache.clear() }

        inline fun <reified T : RebarRecipe> searchRecipes(recipeType: RecipeType<out T>, hint: NamespacedKey?, hash: Int, pred: Predicate<T>): T? {
            // Try the hint (usu. what minecraft thinks we are trying to craft)
            if (hint != null) {
                val hintRecipe = recipeType.getRecipe(hint)
                if (hintRecipe != null && pred.test(hintRecipe)) {
                    cache(hash, recipeType, hintRecipe)
                    return hintRecipe
                }
            }
            // Try the cache
            if (isCached(hash, recipeType)) {
                // Null in the cache means no recipe was found
                val cachedRecipe = getCached(hash, recipeType) ?: return null
                if (cachedRecipe is T && pred.test(cachedRecipe)) return cachedRecipe
            }
            // Linear search
            for (recipe in recipeType.recipes) {
                if (pred.test(recipe)) {
                    cache(hash, recipeType, recipe)
                    return recipe
                }
            }
            cache(hash, recipeType, null)
            return null
        }

        inline fun <reified T : RebarRecipe> searchRecipes(recipeType: RecipeType<out T>, hash: Int, pred: Predicate<T>): T?
            = searchRecipes(recipeType, null, hash, pred)

        fun hashShaped2D(items: Iterable<Iterable<ItemStack?>>): Int =
            items.fold(1) { outerHash, row ->
                row.fold(outerHash) { hash, item -> 31 * hash + (item?.hashIgnoreAmount() ?: 0) }
            }

        fun hashShaped(items: Iterable<ItemStack?>): Int =
            items.fold(1) { hash, i -> 31 * hash + (i?.hashIgnoreAmount() ?: 0) }

        fun hashShapeless2D(items: Iterable<Iterable<ItemStack?>>): Int =
            items.sumOf { row -> row.sumOf { it?.hashIgnoreAmount() ?: 0 } }

        fun hashShapeless(items: Iterable<ItemStack?>): Int =
            items.sumOf { it?.hashIgnoreAmount() ?: 0 }

        fun matchesShaped(items: List<ItemStack?>, recipeInput: List<RecipeInput.Item>, w: Int, h: Int): Boolean {
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

        fun matchesShapeless(items: List<ItemStack?>, recipeInput: List<RecipeInput.Item>): Boolean {
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

    }
}
