package io.github.pylonmc.rebar.recipe

import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemSchema
import io.github.pylonmc.rebar.item.base.*
import io.github.pylonmc.rebar.item.research.Research.Companion.canCraft
import io.github.pylonmc.rebar.nms.NmsAccessor
import io.github.pylonmc.rebar.recipe.RecipeType.Companion.vanillaCraftingRecipes
import io.github.pylonmc.rebar.recipe.vanilla.CookingRecipeWrapper
import io.github.pylonmc.rebar.recipe.vanilla.CraftingRecipeWrapper
import io.github.pylonmc.rebar.recipe.vanilla.VanillaRecipeType
import io.github.pylonmc.rebar.recipe.vanilla.recipeType
import io.github.pylonmc.rebar.util.hashIgnoreAmount
import io.github.pylonmc.rebar.util.isRebarAndIsNot
import io.github.pylonmc.rebar.util.plainText
import io.github.pylonmc.rebar.util.rebarKey
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.event.player.CartographyItemEvent
import net.kyori.adventure.text.Component
import org.bukkit.GameMode
import org.bukkit.Keyed
import org.bukkit.block.Block
import org.bukkit.block.Crafter
import org.bukkit.block.Furnace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockCookEvent
import org.bukkit.event.block.CampfireStartEvent
import org.bukkit.event.block.CrafterCraftEvent
import org.bukkit.event.inventory.*
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.StonecutterInventory
import org.bukkit.inventory.*
import kotlin.math.max
import kotlin.math.min

/**
 * Rebar may add recipes with items that ignore certain components, but there is no
 * RecipeChoice provided in vanilla that accomplishes this so it has to be
 * broadened to MaterialChoice. This means that recipes returned by vanilla matching
 * may be incorrect and will have to be verified manually. If it turns out MC was wrong,
 * We will need to search manually as well.
 */
internal object RebarRecipeListener : Listener {

    private val crafterResultCorrector = rebarKey("crafter_result_corrector")

    @Suppress("UnstableApiUsage")
    @EventHandler(priority = EventPriority.LOWEST)
    private fun onPreCraft(e: PrepareItemCraftEvent) {
        val recipe = e.recipe
        // All recipe types but MerchantRecipe implement Keyed
        if (recipe !is Keyed) return
        val inventory = e.inventory

        val hasRebarItems = inventory.any { it.isRebarAndIsNot<VanillaCraftingItem>() }

        // If vanilla ingredients matched a vanilla recipe, we leave it
        if (recipe.key in VanillaRecipeType.nonRebarRecipes && !hasRebarItems) {
            return
        }

        // Allow merging Rebar tools/weapons/armour in crafting grid unless marked with RebarUnmergeable
        if (hasRebarItems && e.isRepair) {
            var firstItem: ItemStack? = null
            var secondItem: ItemStack? = null
            for (item in e.inventory.matrix) {
                if (item != null && !item.isEmpty) {
                    if (firstItem == null) {
                        firstItem = item
                    } else if (secondItem == null) {
                        secondItem = item
                    } else {
                        error("How the hell is it possible that there are more than two items in an item repair recipe")
                    }
                }
            }

            check(firstItem != null)
            check(secondItem != null)

            val firstSchema = RebarItemSchema.fromStack(firstItem)
            val secondSchema = RebarItemSchema.fromStack(secondItem)
            if (firstSchema == null || secondSchema == null || firstSchema != secondSchema
                || firstSchema.isType(RebarUnmergeable::class.java)
                || firstItem.amount != 1 || secondItem.amount != 1
                || firstItem.hasData(DataComponentTypes.UNBREAKABLE) || secondItem.hasData(DataComponentTypes.UNBREAKABLE)
                || !firstItem.hasData(DataComponentTypes.MAX_DAMAGE) || !secondItem.hasData(DataComponentTypes.MAX_DAMAGE)
                || !firstItem.hasData(DataComponentTypes.DAMAGE) || !secondItem.hasData(DataComponentTypes.DAMAGE)) {
                inventory.result = null
                return
            }

            val resultItem = firstSchema.getItemStack()
            val durability = max(firstItem.getData(DataComponentTypes.MAX_DAMAGE)!!, secondItem.getData(DataComponentTypes.MAX_DAMAGE)!!)
            val firstRemaining = firstItem.getData(DataComponentTypes.MAX_DAMAGE)!! - firstItem.getData(DataComponentTypes.DAMAGE)!!
            val secondRemaining = secondItem.getData(DataComponentTypes.MAX_DAMAGE)!! - secondItem.getData(DataComponentTypes.DAMAGE)!!
            val remaining = firstRemaining + secondRemaining + (durability * 5 / 100) // Based off of NMS

            resultItem.setData(DataComponentTypes.MAX_DAMAGE, durability)
            resultItem.setData(DataComponentTypes.DAMAGE, max(durability - remaining, 0))

            for (enchantment in firstItem.enchantments.keys.union(secondItem.enchantments.keys)) {
                if (enchantment.isCursed) {
                    resultItem.addUnsafeEnchantment(enchantment, max(firstItem.getEnchantmentLevel(enchantment), secondItem.getEnchantmentLevel(enchantment)))
                }
            }

            inventory.result = resultItem
        }
        // Due to rebar ingredients possibly needing to ignore components (and thus using MaterialChoice)
        // we can't fully trust that the recipe returned by MC is correct
        val matrix = e.inventory.matrix.toList()
        var rebarRecipe: CraftingRecipeWrapper? = RecipeService.searchRecipes(
            RecipeType.VANILLA_SHAPED,
            recipe.key,
            RecipeService.hashShapedCraftingInput(matrix)
        ) { it.matches(matrix) }
        if (rebarRecipe == null) {
            // Try shapeless instead
            rebarRecipe = RecipeService.searchRecipes(
                RecipeType.VANILLA_SHAPELESS,
                recipe.key,
                RecipeService.hashShapelessCraftingInput(matrix)
            ) { it.matches(matrix) }
        }
        if (rebarRecipe == null) {
            inventory.result = null
            return
        }
        // Prevent crafting of unresearched items
        val resultSchema = RebarItemSchema.fromStack(recipe.result)
        val anyViewerDoesNotHaveResearch = resultSchema != null && e.viewers.none {
            it is Player && it.canCraft(resultSchema, true)
        }
        if (anyViewerDoesNotHaveResearch) {
            inventory.result = null
            return
        }
        inventory.result = rebarRecipe.craftingRecipe.result.clone()
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onCartography(e: CartographyItemEvent) {
        val inventory = e.inventory
        val hasRebarItems = inventory.any { it.isRebarAndIsNot<VanillaCraftingItem>() }

        if (hasRebarItems) {
            inventory.result = null
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onCrafterCraft(e: CrafterCraftEvent) {
        val crafterState = e.block.state as? Crafter ?: return
        val hasRebarItems = crafterState.inventory.any { it.isRebarAndIsNot<VanillaCraftingItem>() }

        // If vanilla ingredients matched a vanilla recipe, we leave it
        if (e.recipe.key in VanillaRecipeType.nonRebarRecipes && !hasRebarItems) {
            return
        }
        val contents = crafterState.inventory.contents.toList()
        var recipe: CraftingRecipeWrapper? = RecipeService.searchRecipes(
            RecipeType.VANILLA_SHAPED,
            e.recipe.key,
            RecipeService.hashShapedCraftingInput(contents)
        ) { it.matches(contents) }
        if (recipe == null) {
            // Try shapeless instead
            recipe = RecipeService.searchRecipes(
                RecipeType.VANILLA_SHAPELESS,
                e.recipe.key,
                RecipeService.hashShapelessCraftingInput(contents)
            ) { it.matches(contents) }
        }
        if (recipe == null) {
            e.isCancelled = true
            return
        }

        e.result = recipe.craftingRecipe.result.clone()
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun itemInsertEvent(e: InventoryClickEvent) {
        val inventory = e.inventory
        if (inventory is StonecutterInventory) {
            val input = inventory.inputItem ?: return

            if (input.isRebarAndIsNot<VanillaCraftingItem>()) {
                e.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onCook(e: BlockCookEvent) {
        val recipeType = RecipeType.getCookingRecipeTypeByMaterial(e.block.type)
        if (recipeType == null) {
            e.isCancelled = true
            return
        }
        val recipe = RecipeService.searchRecipes(recipeType, e.recipe?.key, e.source.hashIgnoreAmount()) {
            it.matches(e.source)
        }
        if (recipe != null) {
            e.result = recipe.recipe.result.clone()
        } else {
            e.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onStartCook(e: FurnaceStartSmeltEvent) {
        val furnace = e.block.state
        if (furnace !is Furnace) return
        val recipeType = RecipeType.getCookingRecipeTypeByMaterial(furnace.type)
        if (recipeType == null) {
            e.totalCookTime = Int.MAX_VALUE
            return
        }
        val recipe = RecipeService.searchRecipes(recipeType, e.recipe.key, e.source.hashIgnoreAmount()) {
            it.matches(e.source)
        }
        if (recipe == null) {
            e.totalCookTime = Int.MAX_VALUE
            return
        }
        val resultSlotItem = furnace.inventory.result
        val canPlaceInOutput = resultSlotItem == null || (recipe.isOutput(resultSlotItem) && resultSlotItem.amount < resultSlotItem.maxStackSize)
        if (!canPlaceInOutput) {
            e.totalCookTime = Int.MAX_VALUE
        }
    }

    @Suppress("UnstableApiUsage")
    @EventHandler(priority = EventPriority.LOWEST)
    private fun onStartCook(e: CampfireStartEvent) {
        val recipe = RecipeService.searchRecipes(
            RecipeType.VANILLA_CAMPFIRE,
            e.recipe.key,
            e.source.hashIgnoreAmount()
        ) {
            it.matches(e.source)
        }
        if (recipe == null) {
            e.totalCookTime = Int.MAX_VALUE
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onFuelBurn(e: FurnaceBurnEvent) {
        if (e.fuel.isRebarAndIsNot<VanillaCookingFuel>()) {
            e.isCancelled = true
            return
        }
        // Doesn't provide the recipe being crafted so we need to search ourselves
        val furnace = (e.block.state as Furnace)
        val input = furnace.inventory.smelting
        if (input == null) {
            e.isCancelled = true
            return
        }
        val recipeType = RecipeType.getCookingRecipeTypeByMaterial(e.block.type)
        if (recipeType == null) {
            e.isCancelled = true
            return
        }
        val recipe = RecipeService.searchRecipes(recipeType, input.hashIgnoreAmount()) { it.matches(input) }
        if (recipe == null) {
            e.isCancelled = true
            return
        }
        // The recipe is already valid because we searched on our end
        val resultSlotItem = furnace.inventory.result
        val canPlaceInOutput = resultSlotItem == null || (recipe.isOutput(resultSlotItem) && resultSlotItem.amount < resultSlotItem.maxStackSize)
        if (!canPlaceInOutput) {
            e.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onSmith(e: PrepareSmithingEvent) {
        val inv = e.inventory
        val recipe = inv.recipe
        if (recipe !is Keyed) return

        // Prevent the erroneous smithing of vanilla items with Rebar ingredients
        val hasRebarItem = inv.inputMineral.isRebarAndIsNot<VanillaSmithingMineral>()
                || inv.inputTemplate.isRebarAndIsNot<VanillaSmithingTemplate>()
        if (hasRebarItem && recipe.key in VanillaRecipeType.nonRebarRecipes) {
            e.result = null
            return
        }

        // Prevent crafting of unresearched items
        val schemaResult = RebarItemSchema.fromStack(recipe.result)
        val anyViewerDoesNotHaveResearch = schemaResult != null && e.viewers.none {
            it is Player && it.canCraft(schemaResult, true)
        }
        if (anyViewerDoesNotHaveResearch) {
            inv.result = null
        }
    }

    @Suppress("UnstableApiUsage")
    @EventHandler(priority = EventPriority.LOWEST)
    private fun onAnvilSlotChanged(e: PrepareAnvilEvent) {
        val player = e.viewers.first() as? Player ?: return
        val inventory = e.inventory
        val firstItem = inventory.firstItem
        val secondItem = inventory.secondItem
        val firstSchema = RebarItemSchema.fromStack(firstItem)
        val secondSchema = RebarItemSchema.fromStack(secondItem)

        if (firstSchema == null && secondSchema == null) {
            // If it's not a rebar item interaction, we don't care about it
            return
        } else if (firstSchema == null) {
            // If it's a vanilla item being manipulated by a rebar item, prevent it unless it's a VanillaAnvilItem
            if (secondSchema != null && !secondSchema.isType(VanillaAnvilItem::class.java)) {
                e.result = null
            }
            return
        } else if (secondItem == null || secondItem.isEmpty) {
            // If it's renaming a rebar item, allow it, otherwise cancel
            val resultItem = inventory.result
            if (resultItem != null && !firstItem!!.matchesWithoutData(resultItem, setOf(DataComponentTypes.CUSTOM_NAME))) {
                e.result = null
            }
            return
        } else if (firstItem == null) {
            // Dummy check
            return
        }

        var resultItem: ItemStack? = null

        // Allow repairing with rebar items
        if (firstSchema.isType(RebarRepairable::class.java) && firstItem.isRepairable()) {
            val repairable = RebarItem.fromStack(firstItem, RebarRepairable::class.java)!!
            if (repairable.isValidRepairItem(secondItem)) {
                var price = 0
                var namingPrice = 0
                val tax = firstItem.getDataOrDefault(DataComponentTypes.REPAIR_COST, 0)!! + secondItem.getDataOrDefault(DataComponentTypes.REPAIR_COST, 0)!!
                var repairItemCountCost = 0

                resultItem = firstItem.clone()
                var damage = resultItem.getDataOrDefault(DataComponentTypes.DAMAGE, 0)!!
                val maxDamage = resultItem.getDataOrDefault(DataComponentTypes.MAX_DAMAGE, 0)!!
                var repairAmount = min(damage, maxDamage / 4)
                if (repairAmount <= 0) {
                    e.result = null
                    return
                }

                for (i in 0 until secondItem.amount) {
                    if (repairAmount <= 0) break
                    damage -= repairAmount
                    repairAmount = min(damage, maxDamage / 4)
                    repairItemCountCost++
                    price++
                }

                val renameText = e.view.renameText
                if (!renameText.isNullOrBlank()) {
                    if (renameText != resultItem.effectiveName().plainText) {
                        namingPrice = 1
                        price += namingPrice
                        resultItem.setData(DataComponentTypes.CUSTOM_NAME, Component.text(renameText))
                    }
                } else if (resultItem.hasData(DataComponentTypes.CUSTOM_NAME)) {
                    namingPrice = 1
                    price += namingPrice
                    resultItem.unsetData(DataComponentTypes.CUSTOM_NAME)
                }

                if (price <= 0) {
                    e.result = null
                    return
                }

                var cost = Math.clamp((tax + price).toLong(), 0, Int.MAX_VALUE)
                if (namingPrice == price) {
                    if (cost >= 40) {
                        cost = 39
                    }
                }

                e.result = resultItem
                e.view.repairCost = cost
                e.view.repairItemCountCost = repairItemCountCost

                if (cost >= 40 && player.gameMode != GameMode.CREATIVE) {
                    e.result = null
                    return
                }

                var resultCost = resultItem.getDataOrDefault(DataComponentTypes.REPAIR_COST, 0)!!
                val secondCost = secondItem.getDataOrDefault(DataComponentTypes.REPAIR_COST, 0)!!
                if (resultCost < secondCost) {
                    resultCost = secondCost
                }
                resultCost = min(resultCost * 2L + 1L, Int.MAX_VALUE.toLong()).toInt()
                resultItem.setData(DataComponentTypes.DAMAGE, damage)
                resultItem.setData(DataComponentTypes.REPAIR_COST, resultCost)
            }
        }

        // If it hasn't been repaired, check for enchantment application or item merging by piggy backing off of vanilla logic
        if (resultItem == null) {
            resultItem = e.result
            if (resultItem == null) {
                // Something else has already canceled it
                return
            }

            val usingBook = secondItem.hasData(DataComponentTypes.STORED_ENCHANTMENTS)
            if (!usingBook && (firstSchema != secondSchema || firstSchema.isType(RebarUnmergeable::class.java))) {
                e.result = null
            } else if (!firstItem.matchesWithoutData(resultItem, setOf(
                    DataComponentTypes.ENCHANTMENTS, DataComponentTypes.STORED_ENCHANTMENTS,
                    DataComponentTypes.MAX_DAMAGE, DataComponentTypes.DAMAGE,
                    DataComponentTypes.CUSTOM_NAME, DataComponentTypes.REPAIR_COST
            ))) {
                e.result = null
            }
        }
    }

    @Suppress("UnstableApiUsage")
    private fun ItemStack.isRepairable(): Boolean {
        return !hasData(DataComponentTypes.UNBREAKABLE)
                && hasData(DataComponentTypes.MAX_DAMAGE)
                && hasData(DataComponentTypes.DAMAGE)
                && getData(DataComponentTypes.DAMAGE)!! > 0
    }

    internal object DummyRecipe : Recipe {
        override fun getResult(): ItemStack {
            return ItemStack.empty()
        }
    }
}
