package io.github.pylonmc.rebar.recipe

import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemSchema
import io.github.pylonmc.rebar.item.interfaces.*
import io.github.pylonmc.rebar.item.research.Research.Companion.canCraft
import io.github.pylonmc.rebar.nms.NmsAccessor
import io.github.pylonmc.rebar.recipe.RecipeType.Companion.vanillaCraftingRecipes
import io.github.pylonmc.rebar.recipe.vanilla.CookingRecipeWrapper
import io.github.pylonmc.rebar.recipe.vanilla.VanillaRecipeType
import io.github.pylonmc.rebar.recipe.vanilla.recipeType
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
import org.bukkit.event.block.CrafterCraftEvent
import org.bukkit.event.inventory.*
import org.bukkit.inventory.*
import kotlin.math.max
import kotlin.math.min

internal object RebarRecipeListener : Listener {

    private val crafterResultCorrector = rebarKey("crafter_result_corrector")

    @Suppress("UnstableApiUsage")
    @EventHandler(priority = EventPriority.LOWEST)
    private fun onPreCraft(e: PrepareItemCraftEvent) {
        val recipe = e.recipe
        // All recipe types but MerchantRecipe implement Keyed
        if (recipe !is Keyed) return
        val inventory = e.inventory

        val hasRebarItems = inventory.any { it.isRebarAndIsNot<VanillaCraftingIngredientItem>() }
        val isNotRebarCraftingRecipe = recipe.key in VanillaRecipeType.nonRebarRecipes

        // Prevent the erroneous crafting of vanilla items with Rebar ingredients
        if (hasRebarItems && isNotRebarCraftingRecipe) {
            inventory.result = null
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
                || firstSchema.isType(UnmergeableRebarItem::class.java)
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

        // Prevent crafting of unresearched items
        val resultSchema = RebarItemSchema.fromStack(recipe.result)
        val anyViewerDoesNotHaveResearch = resultSchema != null && e.viewers.none {
            it is Player && it.canCraft(resultSchema, true)
        }
        if (anyViewerDoesNotHaveResearch) {
            inventory.result = null
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onCartography(e: CartographyItemEvent) {
        val inventory = e.inventory
        val hasRebarItems = inventory.any { it.isRebarAndIsNot<VanillaCraftingIngredientItem>() }

        if (hasRebarItems) {
            inventory.result = null
        }
    }

    private fun getCorrectedCrafterRecipe(originalRecipe: Recipe?, block: Block): Recipe? {
        if (originalRecipe is Keyed && originalRecipe.key !in VanillaRecipeType.nonRebarRecipes) {
            // Already a rebar recipe, so it should be valid
            return originalRecipe
        }

        val crafter = block.getState(false) as? Crafter ?: return null
        val inventory = crafter.inventory

        val hasRebarItems = inventory.any { it.isRebarAndIsNot<VanillaCraftingIngredientItem>() }
        if (!hasRebarItems) {
            return originalRecipe
        }

        // TODO make this not horrible (both for performance and readability) - see https://github.com/pylonmc/rebar/issues/545
        for (recipe in vanillaCraftingRecipes()) {
            val craftingRecipe = recipe.craftingRecipe
            if (craftingRecipe.key in VanillaRecipeType.nonRebarRecipes) {
                continue
            }

            if (craftingRecipe is ShapedRecipe) {
                var i = 0
                var isValid = true
                rowLoop@ for (row in craftingRecipe.shape) {
                    ingredientLoop@ for (index in row) {
                        val ingredient = craftingRecipe.choiceMap[index]
                        val actual = inventory.getItem(i++)
                        if (ingredient == null && (actual == null || actual.isEmpty)) {
                            continue@ingredientLoop
                        }

                        if (ingredient == null || actual == null || !ingredient.test(actual)) {
                            isValid = false
                            break@rowLoop
                        }
                    }
                }
                if (isValid) {
                    return craftingRecipe
                }
            } else if (craftingRecipe is ShapelessRecipe) {
                val slots = crafter.inventory.contents.filterNotNull().toMutableList()
                var isValid = true
                ingredientLoop@ for (ingredient in craftingRecipe.choiceList) {
                    var found = false
                    slotLoop@ for (crafterIndex in slots.indices) {
                        val actual = slots[crafterIndex]
                        if (!ingredient.test(actual)) {
                            continue
                        }
                        found = true
                        slots.removeAt(crafterIndex)
                        break@slotLoop
                    }

                    if (!found) {
                        isValid = false
                        break@ingredientLoop
                    }
                }
                if (isValid && slots.isEmpty()) {
                    return craftingRecipe
                }
            }
        }
        return null
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onOpenCrafter(e: InventoryOpenEvent) {
        val slotListener: NmsAccessor.SlotListener = slotListener@ { inventoryView, _, _, _ ->
            val crafterInventory = inventoryView.topInventory as? CrafterInventory ?: return@slotListener
            val block = crafterInventory.location?.block ?: return@slotListener
            // We do not have the original recipe so instead we pass a dummy recipe and check if
            // that is what is returned, if so, whatever the result already is, is valid
            // otherwise it should be corrected to the found recipe's result, or null
            val correctedRecipe = getCorrectedCrafterRecipe(DummyRecipe, block)
            if (correctedRecipe === DummyRecipe) return@slotListener
            crafterInventory.setItem(9, correctedRecipe?.result)
        }
        slotListener(e.view, 0, null, null)
        NmsAccessor.instance.addSlotChangedListener(crafterResultCorrector, e.view, slotListener)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onCrafterCraft(e: CrafterCraftEvent) {
        val correctedRecipe = getCorrectedCrafterRecipe(e.recipe, e.block)
        if (correctedRecipe != null && correctedRecipe !== e.recipe) {
            e.result = correctedRecipe.result
        } else if (correctedRecipe == null) {
            e.isCancelled = true
            e.result = ItemStack.empty()
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun itemInsertEvent(e: InventoryClickEvent) {
        val inventory = e.inventory;
        if (inventory is StonecutterInventory) {
            val input = inventory.inputItem ?: return

            if (input.isRebarAndIsNot<VanillaCraftingIngredientItem>()) {
                e.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onStartCook(e: FurnaceStartSmeltEvent) {
        if (RebarItemSchema.fromStack(e.source) == null) return

        val originalRecipe = e.recipe
        if (originalRecipe.key !in VanillaRecipeType.nonRebarRecipes) {
            return
        }

        val originalType = originalRecipe.recipeType
        if (originalType == null) {
            e.totalCookTime = 0 // instantly complete so that it doesn't show progress bar, this will get canceled in BlockCookEvent
            return
        }

        for (recipe in originalType.recipes) {
            if (recipe is CookingRecipeWrapper && recipe.key !in VanillaRecipeType.nonRebarRecipes && recipe.recipe.inputChoice.test(e.source)) {
                e.totalCookTime = recipe.recipe.cookingTime
                NmsAccessor.instance.setFurnaceRecipeCache(e.block, recipe.key)
                break
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onCook(e: BlockCookEvent) {
        if (RebarItemSchema.fromStack(e.source) == null) return

        val originalRecipe = e.recipe
        if (originalRecipe == null) {
            e.isCancelled = true
            return
        }

        if (originalRecipe.key !in VanillaRecipeType.nonRebarRecipes) {
            // already handled correctly
            return
        }

        val originalType = originalRecipe.recipeType
        if (originalType == null) {
            e.isCancelled = true
            return
        }

        for (recipe in originalType.recipes) {
            if (recipe is CookingRecipeWrapper && recipe.key !in VanillaRecipeType.nonRebarRecipes && recipe.recipe.inputChoice.test(e.source)) {
                e.result = recipe.recipe.result
                NmsAccessor.instance.setFurnaceRecipeCache(e.block, recipe.key)
                break
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onFuelBurn(e: FurnaceBurnEvent) {
        if (e.fuel.isRebarAndIsNot<VanillaFurnaceFuel>()) {
            e.isCancelled = true
            return
        }

        val furnace = (e.block.state as Furnace)
        val input = furnace.inventory.smelting
        if (input != null && input.isRebarAndIsNot<VanillaFurnaceIngredientItem>()) {
            var rebarRecipe: CookingRecipeWrapper? = null
            for (recipe in RecipeType.vanillaCookingRecipes()) {
                if (recipe.key !in VanillaRecipeType.nonRebarRecipes && recipe.recipe.inputChoice.test(input)) {
                    rebarRecipe = recipe
                    break
                }
            }
            val isFurnaceOutputValidToPutRecipeResultIn = rebarRecipe != null
                    && (furnace.inventory.result == null || rebarRecipe.isOutput(furnace.inventory.result!!))
            if (rebarRecipe == null || !isFurnaceOutputValidToPutRecipeResultIn) {
                e.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onSmith(e: PrepareSmithingEvent) {
        val inv = e.inventory
        val recipe = inv.recipe
        if (recipe !is Keyed) return

        // Prevent the erroneous smithing of vanilla items with Rebar ingredients
        val hasRebarItem = inv.inputEquipment.isRebarAndIsNot<VanillaSmithingBase>()
                || inv.inputMineral.isRebarAndIsNot<VanillaSmithingMaterial>()
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
            if (secondSchema != null && !secondSchema.isType(VanillaAnvilUseItem::class.java)) {
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
        if (firstSchema.isType(RepairableRebarItem::class.java) && firstItem.isRepairable()) {
            val repairable = RebarItem.fromStack(firstItem, RepairableRebarItem::class.java)!!
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
            if (!usingBook && (firstSchema != secondSchema || firstSchema.isType(UnmergeableRebarItem::class.java))) {
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
