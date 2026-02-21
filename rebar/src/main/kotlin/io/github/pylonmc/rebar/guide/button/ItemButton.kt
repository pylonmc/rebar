package io.github.pylonmc.rebar.guide.button

import com.github.shynixn.mccoroutine.bukkit.launch
import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.guide.pages.item.ItemRecipesPage
import io.github.pylonmc.rebar.guide.pages.item.ItemUsagesPage
import io.github.pylonmc.rebar.guide.pages.research.ResearchItemsPage
import io.github.pylonmc.rebar.i18n.RebarArgument
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.item.research.Research.Companion.canCraft
import io.github.pylonmc.rebar.item.research.Research.Companion.canUse
import io.github.pylonmc.rebar.item.research.Research.Companion.guideHints
import io.github.pylonmc.rebar.item.research.Research.Companion.researchPoints
import io.github.pylonmc.rebar.recipe.RecipeInput
import io.github.pylonmc.rebar.util.gui.unit.UnitFormat
import io.papermc.paper.datacomponent.DataComponentTypes
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
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
import kotlin.time.Duration.Companion.seconds

/**
 * Represents an item in the guide.
 *
 * @param stacks The items to display. If multiple are provided, the button will automatically
 * cycle through all of them. You must supply at least one item
 */
class ItemButton @JvmOverloads constructor(
    stacks: List<ItemStack>,

    /**
     * A function to apply to the button item after creating it.
     */
    val preDisplayDecorator: (ItemStack, Player) -> ItemStack = { stack, _ -> stack }
) : AbstractBoundItem() {

    /**
     * @param stacks The items to display. If multiple are provided, the button will automatically
     * cycle through all of them. You must supply at least one item
     */
    constructor(vararg stacks: ItemStack) : this(stacks.toList())

    /**
     * @param stacks The items to display. If multiple are provided, the button will automatically
     * cycle through all of them. You must supply at least one item
     */
    constructor(stack: ItemStack, preDisplayDecorator: (ItemStack, Player) -> ItemStack) : this(listOf(stack), preDisplayDecorator)

    val stacks = stacks.shuffled()
    private var index = 0
    val currentStack: ItemStack
        get() = this.stacks[index]

    init {
        require(stacks.isNotEmpty()) { "ItemButton must have at least one ItemStack" }
        if (stacks.size > 1) {
            Rebar.launch {
                while (true) {
                    delay(1.seconds)
                    index += 1
                    index %= stacks.size
                    notifyWindows()
                }
            }
        }
    }

    @Suppress("UnstableApiUsage")
    override fun getItemProvider(player: Player): ItemProvider {
        try {
            val displayStack = preDisplayDecorator.invoke(currentStack.clone(), player)
            val item = RebarItem.fromStack(displayStack) ?: return ItemStackBuilder.of(displayStack)

            val builder = ItemStackBuilder.of(displayStack)
            if (item.isDisabled) {
                builder.set(DataComponentTypes.ITEM_MODEL, Material.STRUCTURE_VOID.key)
            }

            if (!player.canCraft(item, respectBypass = false)) {
                builder.set(DataComponentTypes.ITEM_MODEL, Material.BARRIER.key)
                    .set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false)

                val research = item.research
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
                        Component.translatable("rebar.guide.button.item.not-researched")
                    }

                    val lore = builder.lore()?.lines()?.toMutableList() ?: mutableListOf()
                    lore.add(0, loreLine)
                    builder.clearLore()
                    builder.lore(lore)
                }
            }

            if (player.guideHints) {
                if (!player.canCraft(item, respectBypass = false)) {
                    builder.lore(Component.translatable("rebar.guide.button.item.hints.unresearched"))
                } else {
                    builder.lore(Component.translatable("rebar.guide.button.item.hints.researched"))
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
                    }
                }

                ClickType.SHIFT_LEFT -> {
                    val item = RebarItem.fromStack(currentStack)
                    val research = item?.research
                    if (item != null && research != null) {
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
                    }
                }

                ClickType.RIGHT -> {
                    val page = ItemUsagesPage(currentStack)
                    if (page.pages.isNotEmpty()) {
                        page.open(player)
                    }
                }

                ClickType.SHIFT_RIGHT -> {
                    val item = RebarItem.fromStack(currentStack)
                    if (item != null && item.research != null && !player.canUse(item)) {
                        ResearchItemsPage(item.research!!).open(player)
                    }
                }

                ClickType.MIDDLE -> {
                    if (!player.hasPermission("rebar.guide.cheat")) return
                    val stack = getCheatItemStack(currentStack, click)
                    stack.amount = stack.maxStackSize
                    player.setItemOnCursor(stack)
                }

                ClickType.DROP -> {
                    if (!player.hasPermission("rebar.guide.cheat")) return
                    val stack = getCheatItemStack(currentStack, click)
                    stack.amount = 1
                    if (player.itemOnCursor.isEmpty) {
                        player.setItemOnCursor(stack)
                    } else if (player.itemOnCursor.isSimilar(stack)) {
                        player.itemOnCursor.add()
                    }
                }

                ClickType.CONTROL_DROP -> {
                    if (!player.hasPermission("rebar.guide.cheat")) return
                    val stack = getCheatItemStack(currentStack, click)
                    stack.amount = stack.maxStackSize
                    player.dropItem(stack)
                }

                ClickType.SWAP_OFFHAND -> {
                    if (!player.hasPermission("rebar.guide.cheat")) return
                    val stack = getCheatItemStack(currentStack, click)
                    stack.amount = 1
                    player.give(stack)
                }

                else -> {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private fun getCheatItemStack(currentStack: ItemStack, click: Click): ItemStack {
            val clonedUnkown = currentStack.clone()
            val rebarItem = RebarItem.fromStack(clonedUnkown)

            if (rebarItem == null) {
                // Item is not Rebar
                val type = Registry.MATERIAL.get(clonedUnkown.type.key)!!
                val amount = if (click.clickType.isShiftClick) { type.maxStackSize } else { 1 }
                val clonedNotRebar = ItemStack(type, amount)
                return clonedNotRebar
            } else {
                // Rebar item handling
                val clonedRebar = rebarItem.schema.getItemStack()
                clonedRebar.amount = if (click.clickType.isShiftClick) { clonedRebar.maxStackSize } else { 1 }
                return clonedRebar
            }
        }

        @JvmStatic
        fun from(stack: ItemStack?): Item {
            if (stack == null) {
                return EMPTY
            }

            return ItemButton(stack)
        }

        @JvmStatic
        fun from(stack: ItemStack?, preDisplayDecorator: (ItemStack, Player) -> ItemStack):  Item {
            if (stack == null) {
                return EMPTY
            }

            return ItemButton(listOf(stack), preDisplayDecorator)
        }

        @JvmStatic
        fun from(input: RecipeInput.Item?): Item {
            if (input == null) {
                return EMPTY
            }

            return ItemButton(*input.representativeItems.toTypedArray())
        }

        @JvmStatic
        fun from(choice: RecipeChoice?): Item = when (choice) {
            is RecipeChoice.MaterialChoice -> ItemButton(choice.choices.map(::ItemStack))
            is RecipeChoice.ExactChoice -> ItemButton(choice.choices)
            else -> EMPTY
        }
    }
}