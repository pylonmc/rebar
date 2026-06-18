package io.github.pylonmc.rebar.guide.button

import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.guide.pages.item.ItemRecipesPage
import io.github.pylonmc.rebar.guide.pages.item.ItemUsagesPage
import io.github.pylonmc.rebar.guide.pages.research.ResearchItemsPage
import io.github.pylonmc.rebar.i18n.RebarArgument
import io.github.pylonmc.rebar.item.ItemTypeWrapper
import io.github.pylonmc.rebar.item.RebarItemSchema
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.item.research.Research.Companion.canCraft
import io.github.pylonmc.rebar.item.research.Research.Companion.canUse
import io.github.pylonmc.rebar.content.guide.RebarGuide.Companion.guideHints
import io.github.pylonmc.rebar.content.guide.RebarGuide.Companion.playGuideSound
import io.github.pylonmc.rebar.item.research.Research.Companion.researchPoints
import io.github.pylonmc.rebar.recipe.RecipeInput
import io.github.pylonmc.rebar.util.gui.unit.UnitFormat
import io.papermc.paper.datacomponent.DataComponentTypes
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Registry
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import xyz.xenondevs.invui.Click
import xyz.xenondevs.invui.gui.SlotElement
import xyz.xenondevs.invui.gui.get
import xyz.xenondevs.invui.item.AbstractBoundItem
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemProvider

/**
 * Represents an item in the guide.
 *
 * @param stacks The items to display. If multiple are provided, the button will automatically
 * cycle through all of them. You must supply at least one item
 */
class ItemButton private constructor(
    stacks: List<ItemStack>,

    /**
     * A function to apply to the button item after creating it.
     */
    val preDisplayDecorator: Decorator = noDecorator
) : AbstractBoundItem() {

    val stacks = stacks.shuffled()
    val currentStack: ItemStack
        get() = this.stacks[(Bukkit.getCurrentTick() / 20) % this.stacks.size]

    init {
        require(stacks.isNotEmpty()) { "ItemButton must have at least one ItemStack" }
    }

    override fun getUpdatePeriod(what: Int): Int = if (stacks.size > 1) 20 else -1

    @Suppress("UnstableApiUsage")
    override fun getItemProvider(player: Player): ItemProvider {
        try {
            val displayStack = preDisplayDecorator.invoke(currentStack.clone(), player)
            val itemSchema = RebarItemSchema.fromStack(displayStack) ?: return ItemStackBuilder.of(displayStack)

            val builder = ItemStackBuilder.of(displayStack)
            if (itemSchema.isDisabled) {
                builder.set(DataComponentTypes.ITEM_MODEL, Material.STRUCTURE_VOID.key)
            }

            if (!player.canCraft(itemSchema)) {
                builder.set(DataComponentTypes.ITEM_MODEL, Material.BARRIER.key)
                    .set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false)

                val research = itemSchema.research
                if (research != null) {
                    val loreLine = if (research.cost != null) {
                        val playerPoints = player.researchPoints
                        Component.translatable(
                            "rebar.guide.button.item.not-researched."
                                    + (if (research.cost > playerPoints) "not-enough-points" else "enough-points"),
                            RebarArgument.of("research_name", research.name),
                            RebarArgument.of("player_points", playerPoints),
                            RebarArgument.of("unlock_cost", UnitFormat.RESEARCH_POINTS.format(research.cost))
                        )
                    } else {
                        // Null-cost research uses a custom unlock flow defined by the addon.
                        // Delegate to the addon's unlock-instructions key, mirroring
                        // ResearchButton's handling of the same case.
                        Component.translatable(
                            "${research.key.namespace}.researches.${research.key.key}.unlock-instructions"
                        )
                    }

                    val lore = builder.lore()?.lines()?.toMutableList() ?: mutableListOf()
                    lore.add(0, loreLine)
                    builder.clearLore()
                    builder.lore(lore)
                }
            }

            if (player.guideHints) {
                if (!player.canCraft(itemSchema)) {
                    builder.lore(Component.translatable("rebar.guide.button.item.hints.unresearched"))
                }
            }

            return builder
        } catch (e: Exception) {
            e.printStackTrace()
            return ItemStackBuilder.of(Material.BARRIER)
                .name(Component.translatable("rebar.guide.button.item.error"))
        }
    }

    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        try {
            when (clickType) {
                ClickType.LEFT -> {
                    val page = ItemRecipesPage(currentStack)
                    if (page.pages.isNotEmpty()) {
                        page.open(player)
                        player.playGuideSound(RebarConfig.GuideConfig.CLICK_BUTTON_SOUND)
                    }
                }

                ClickType.SHIFT_LEFT -> {
                    val itemSchema = RebarItemSchema.fromStack(currentStack)
                    val research = itemSchema?.research
                    if (itemSchema != null && research != null) {
                        if (research.isResearchedBy(player) || research.cost == null || research.cost > player.researchPoints) {
                            return
                        }
                        research.addTo(player, false)
                        player.researchPoints -= research.cost
                        for (slot in 0 until gui.size) {
                            val item = gui[slot] as? SlotElement.Item ?: continue
                            if (item.item is ItemButton) {
                                item.item.notifyWindows()
                            }
                        }
                        player.playGuideSound(RebarConfig.GuideConfig.CLICK_BUTTON_SOUND)
                    }
                }

                ClickType.RIGHT -> {
                    val page = ItemUsagesPage(currentStack)
                    if (page.pages.isNotEmpty()) {
                        page.open(player)
                        player.playGuideSound(RebarConfig.GuideConfig.CLICK_BUTTON_SOUND)
                    }
                }

                ClickType.SHIFT_RIGHT -> {
                    val itemSchema = RebarItemSchema.fromStack(currentStack)
                    if (itemSchema != null && itemSchema.research != null && !player.canUse(itemSchema)) {
                        ResearchItemsPage(itemSchema.research!!).open(player)
                        player.playGuideSound(RebarConfig.GuideConfig.CLICK_BUTTON_SOUND)
                    }
                }

                ClickType.MIDDLE -> {
                    if (!player.hasPermission("rebar.guide.cheat")) return
                    val stack = getCheatItemStack(currentStack, click)
                    stack.amount = stack.maxStackSize
                    player.setItemOnCursor(stack)
                    player.playGuideSound(RebarConfig.GuideConfig.CLICK_BUTTON_SOUND)
                }

                ClickType.DROP -> {
                    if (!player.hasPermission("rebar.guide.cheat")) return
                    val stack = getCheatItemStack(currentStack, click)
                    stack.amount = 1
                    if (player.itemOnCursor.isEmpty) {
                        player.setItemOnCursor(stack)
                        player.playGuideSound(RebarConfig.GuideConfig.CLICK_BUTTON_SOUND)
                    } else if (player.itemOnCursor.isSimilar(stack)) {
                        player.itemOnCursor.add()
                        player.playGuideSound(RebarConfig.GuideConfig.CLICK_BUTTON_SOUND)
                    }
                }

                ClickType.CONTROL_DROP -> {
                    if (!player.hasPermission("rebar.guide.cheat")) return
                    val stack = getCheatItemStack(currentStack, click)
                    stack.amount = stack.maxStackSize
                    player.dropItem(stack)
                    player.playGuideSound(RebarConfig.GuideConfig.CLICK_BUTTON_SOUND)
                }

                ClickType.SWAP_OFFHAND -> {
                    if (!player.hasPermission("rebar.guide.cheat")) return
                    val stack = getCheatItemStack(currentStack, click)
                    stack.amount = 1
                    player.give(stack)
                    player.playGuideSound(RebarConfig.GuideConfig.CLICK_BUTTON_SOUND)
                }

                else -> {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hasItemType(itemTypeWrapper: ItemTypeWrapper): Boolean {
        for (stack in stacks) {
            if (itemTypeWrapper.matches(stack)) {
                return true
            }
        }
        return false
    }

    companion object {
        private typealias Decorator = (ItemStack, Player) -> ItemStack
        private val noDecorator: Decorator = { stack, _ -> stack }

        @Suppress("UnstableApiUsage")
        private fun getCheatItemStack(currentStack: ItemStack, click: Click): ItemStack {
            val itemSchema = RebarItemSchema.fromStack(currentStack)
            if (itemSchema == null) {
                // Item is not Rebar
                val type = Registry.MATERIAL.get(currentStack.type.key)!!
                val amount = if (click.clickType.isShiftClick) type.maxStackSize else 1
                val clonedNotRebar = ItemStack.of(type, amount)
                clonedNotRebar.copyDataFrom(currentStack) {
                    it != DataComponentTypes.ITEM_NAME && it != DataComponentTypes.LORE
                }
                return clonedNotRebar
            } else {
                // Rebar item handling
                val amount = if (click.clickType.isShiftClick) 99 else 1 // the schema will automatically cap the amount to the max stack size
                val clonedRebar = itemSchema.getItemStack(amount)
                return clonedRebar
            }
        }

        @JvmStatic
        @JvmOverloads
        fun of(stack: ItemStack?, preDisplayDecorator: Decorator? = null): Item {
            if (stack == null || stack.isEmpty) {
                return EMPTY
            }

            return ItemButton(listOf(stack), preDisplayDecorator ?: noDecorator)
        }

        @JvmStatic
        fun of(input: RecipeInput.Item?): Item {
            if (input == null) {
                return EMPTY
            }

            return of(input.representativeItems.toList())
        }

        @JvmStatic
        fun of(choice: RecipeChoice?): Item = when (choice) {
            is RecipeChoice.MaterialChoice -> of(choice.choices.map(ItemStack::of))
            is RecipeChoice.ExactChoice -> of(choice.choices)
            else -> EMPTY
        }

        @JvmStatic @JvmOverloads
        fun of(stacks: List<ItemStack?>, preDisplayDecorator: Decorator? = null) = if (stacks.filterNotNull().isEmpty()) {
            EMPTY
        } else {
            ItemButton(stacks.filterNotNull(), preDisplayDecorator ?: noDecorator)
        }

        /**
         * @param stacks The items to display. If multiple are provided, the button will automatically
         * cycle through all of them. (if no items are provided, returns an empty item)
         */
        @JvmStatic
        @JvmOverloads
        fun of(preDisplayDecorator: Decorator? = null, vararg stacks: ItemStack?) = of(stacks.toList(), preDisplayDecorator)
    }
}