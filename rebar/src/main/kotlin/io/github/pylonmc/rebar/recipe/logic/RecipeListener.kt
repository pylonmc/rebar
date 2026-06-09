package io.github.pylonmc.rebar.recipe.logic

import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemSchema
import io.github.pylonmc.rebar.item.interfaces.*
import io.github.pylonmc.rebar.item.research.Research.Companion.canCraft
import io.github.pylonmc.rebar.nms.NmsAccessor
import io.github.pylonmc.rebar.recipe.RecipeType
import io.github.pylonmc.rebar.recipe.vanilla.AbstractCraftingRebarRecipe
import io.github.pylonmc.rebar.recipe.vanilla.CraftingInput
import io.github.pylonmc.rebar.recipe.vanilla.DummyVanillaRebarRecipe
import io.github.pylonmc.rebar.recipe.vanilla.VanillaRecipeType
import io.github.pylonmc.rebar.recipe.vanilla.rebarRecipeType
import io.github.pylonmc.rebar.util.isRebarAndIsNot
import io.github.pylonmc.rebar.util.plainText
import io.github.pylonmc.rebar.util.rebarKey
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.event.player.CartographyItemEvent
import net.kyori.adventure.text.Component
import org.bukkit.GameMode
import org.bukkit.Keyed
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Campfire
import org.bukkit.block.Crafter
import org.bukkit.block.Furnace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockCookEvent
import org.bukkit.event.block.CrafterCraftEvent
import org.bukkit.event.inventory.*
import org.bukkit.event.player.PlayerRecipeDiscoverEvent
import org.bukkit.inventory.*
import kotlin.math.max
import kotlin.math.min

internal object RebarRecipeListener : Listener {

    private val crafterResultCorrector = rebarKey("crafter_result_corrector")

    @Suppress("UnstableApiUsage")
    @EventHandler(priority = EventPriority.LOWEST)
    private fun onPreCraft(e: PrepareItemCraftEvent) {
        val recipe = e.recipe
        val inventory = e.inventory

        val hasRebarItems = inventory.any { it.isRebarAndIsNot<VanillaCraftingIngredientItem>() }
        val isNotRebarCraftingRecipe = recipe == null || recipe !is Keyed || recipe.key in VanillaRecipeType.nonRebarRecipes

        // Prevent the erroneous crafting of vanilla items with Rebar ingredients
        if (hasRebarItems && isNotRebarCraftingRecipe) {
            inventory.result = null
        }
        // Prevent crafting dummy recipes
        else if (RecipeType.isDummyRecipe(recipe)) {
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

        // Use the recipe matcher if necessary
        if (recipe == null || inventory.result == null) {
            val matchedRecipe = RecipeMatchingService.matchCraftingRecipe(CraftingInput.of(inventory), recipe)
            if (matchedRecipe != null) {
                inventory.result = matchedRecipe.result.item.clone()
            }
        }

        // Prevent crafting of unresearched items
        val resultSchema = RebarItemSchema.fromStack(inventory.result)
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

    private fun checkCrafterRecipe(block: Block, possibleRecipe: Recipe? = null): Pair<Boolean, AbstractCraftingRebarRecipe?>? {
        val crafter = block.getState(false) as? Crafter ?: return null
        val inventory = crafter.inventory

        val hasRebarItems = inventory.any { it.isRebarAndIsNot<VanillaCraftingIngredientItem>() }
        if (!hasRebarItems && possibleRecipe != null && !RecipeType.isDummyRecipe(possibleRecipe)) {
            return true to null
        }

        val matchedRecipe = RecipeMatchingService.matchCraftingRecipe(CraftingInput.of(inventory), possibleRecipe)
        return false to matchedRecipe
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onOpenCrafter(e: InventoryOpenEvent) {
        val slotListener: NmsAccessor.SlotListener = slotListener@ { inventoryView, _, _, _ ->
            val crafterInventory = inventoryView.topInventory as? CrafterInventory ?: return@slotListener
            val block = crafterInventory.location?.block ?: return@slotListener
            // We do not have the original recipe so instead we pass a dummy recipe and check if
            // that is what is returned, if so, whatever the result already is, is valid
            // otherwise it should be corrected to the found recipe's result, or null
            val result = checkCrafterRecipe(block)
            if (result != null && result.first) return@slotListener
            crafterInventory.setItem(9, result?.second?.result?.item?.clone())
        }
        slotListener(e.view, 0, null, null)
        NmsAccessor.instance.addSlotChangedListener(crafterResultCorrector, e.view, slotListener)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onCrafterCraft(e: CrafterCraftEvent) {
        val result = checkCrafterRecipe(e.block, e.recipe)
        if (result != null && !result.first) {
            val recipe = result.second
            if (recipe != null) {
                e.result = recipe.result.item
            } else {
                e.isCancelled = true
                e.result = ItemStack.empty()
            }
        } else if (result == null) {
            e.isCancelled = true
            e.result = ItemStack.empty()
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onInsertIntoStonecutter(e: InventoryClickEvent) {
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
        val rebarSource = e.source.isRebarAndIsNot<VanillaFurnaceIngredientItem>()

        val originalRecipe = e.recipe
        val rebarType = originalRecipe.rebarRecipeType
        if (rebarType == null && rebarSource) {
            e.totalCookTime = 0 // instantly complete so that it doesn't show progress bar, this will get canceled in BlockCookEvent
            return
        } else if (rebarType == null) {
            // We don't need to handle this recipe type
            return
        }

        // Prevent crafting dummy recipes by default
        if (RecipeType.isDummyRecipe(originalRecipe)) {
            e.totalCookTime = 0
        }

        val block = e.block
        val furnace = block.getState(false) as? Furnace
        if (furnace == null) {
            e.totalCookTime = 0
            return
        }

        val matchedRecipe = RecipeMatchingService.matchCookingRecipe(rebarType, e.source, furnace.inventory.result, originalRecipe)
        if (matchedRecipe != null) {
            e.totalCookTime = matchedRecipe.cookingTime
            NmsAccessor.instance.setFurnaceRecipeCache(block, DummyVanillaRebarRecipe.recipeKey(matchedRecipe.key))
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onCook(e: BlockCookEvent) {
        val rebarSource = e.source.isRebarAndIsNot<VanillaFurnaceIngredientItem>()

        val originalRecipe = e.recipe
        if (originalRecipe == null) {
            e.isCancelled = true
            return
        }

        val rebarType = originalRecipe.rebarRecipeType
        if (rebarType == null && rebarSource) {
            e.isCancelled = true
            return
        } else if (rebarType == null) {
            // We don't need to handle this
            return
        }

        val block = e.block
        val blockState = block.getState(false)
        val result = when(blockState) {
            is Furnace -> blockState.inventory.result
            is Campfire -> null
            else -> {
                e.isCancelled = true
                return
            }
        }

        val matchedRecipe = RecipeMatchingService.matchCookingRecipe(rebarType, e.source, result, originalRecipe)
        if (matchedRecipe != null) {
            e.result = matchedRecipe.result.item.clone()
            if (blockState is Furnace) {
                NmsAccessor.instance.setFurnaceRecipeCache(e.block, DummyVanillaRebarRecipe.recipeKey(matchedRecipe.key))
            }
            return
        }

        e.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onFuelBurn(e: FurnaceBurnEvent) {
        if (e.fuel.isRebarAndIsNot<VanillaFurnaceFuel>()) {
            e.isCancelled = true
            return
        }

        val block = e.block
        val furnace = block.getState(false) as? Furnace
        if (furnace == null) {
            e.isCancelled = true
            return
        }

        val rebarType = when(block.type) {
            Material.FURNACE -> RecipeType.VANILLA_FURNACE
            Material.SMOKER -> RecipeType.VANILLA_SMOKING
            Material.BLAST_FURNACE -> RecipeType.VANILLA_BLASTING
            else -> {
                e.isCancelled = true
                return
            }
        }

        val source = furnace.inventory.smelting
        val result = furnace.inventory.result
        val matchedRecipe = RecipeMatchingService.matchCookingRecipe(rebarType, source, result, null)
        if (matchedRecipe == null) {
            e.isCancelled = true
            return
        }

        NmsAccessor.instance.setFurnaceRecipeCache(block, DummyVanillaRebarRecipe.recipeKey(matchedRecipe.key))
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onSmith(e: PrepareSmithingEvent) {
        val inv = e.inventory
        val recipe = inv.recipe

        // Prevent the erroneous smithing of with Rebar ingredients in place of vanilla items
        val hasRebarItem = inv.inputTemplate.isRebarAndIsNot<VanillaSmithingTemplate>()
                || inv.inputEquipment.isRebarAndIsNot<VanillaSmithingBase>()
                || inv.inputMineral.isRebarAndIsNot<VanillaSmithingMaterial>()
        if (hasRebarItem && (recipe !is Keyed || recipe.key in VanillaRecipeType.nonRebarRecipes)) {
            e.result = null
        }
         // Prevent crafting dummy recipes
         else if (RecipeType.isDummyRecipe(recipe)) {
            e.result = null
        }

        // Custom recipe matching
        if (recipe == null || e.result == null) {
            val matchedRecipe = RecipeMatchingService.matchSmithingRecipe(inv.inputTemplate, inv.inputEquipment, inv.inputMineral, recipe)
            if (matchedRecipe != null) {
                e.result = matchedRecipe.result.item.clone()
            }
        }

        // Prevent crafting of unresearched items
        val schemaResult = RebarItemSchema.fromStack(e.result)
        val anyViewerDoesNotHaveResearch = schemaResult != null && e.viewers.none {
            it is Player && it.canCraft(schemaResult, true)
        }
        if (anyViewerDoesNotHaveResearch) {
            e.result = null
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

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun onDiscoverDummyRecipe(event: PlayerRecipeDiscoverEvent) {
        event.isCancelled = RecipeType.isDummyRecipe(event.recipe)
    }

    @Suppress("UnstableApiUsage")
    private fun ItemStack.isRepairable(): Boolean {
        return !hasData(DataComponentTypes.UNBREAKABLE)
                && hasData(DataComponentTypes.MAX_DAMAGE)
                && hasData(DataComponentTypes.DAMAGE)
                && getData(DataComponentTypes.DAMAGE)!! > 0
    }
}
