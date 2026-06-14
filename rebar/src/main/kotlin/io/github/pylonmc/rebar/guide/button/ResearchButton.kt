package io.github.pylonmc.rebar.guide.button

import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.guide.pages.research.ResearchItemsPage
import io.github.pylonmc.rebar.i18n.RebarArgument
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.item.research.Research
import io.github.pylonmc.rebar.content.guide.RebarGuide.Companion.guideHints
import io.github.pylonmc.rebar.content.guide.RebarGuide.Companion.playGuideSound
import io.github.pylonmc.rebar.item.research.Research.Companion.researchPoints
import io.github.pylonmc.rebar.util.gui.unit.UnitFormat
import io.github.pylonmc.rebar.util.rebarKey
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.Click
import xyz.xenondevs.invui.gui.SlotElement
import xyz.xenondevs.invui.gui.get
import xyz.xenondevs.invui.item.AbstractBoundItem
import xyz.xenondevs.invui.item.ItemProvider

/**
 * A button that shows a research.
 */
open class ResearchButton(val research: Research) : AbstractBoundItem() {

    override fun getItemProvider(player: Player): ItemProvider = try {
        val playerHasResearch = Research.getResearches(player).contains(research)
        val item = ItemStackBuilder.gui(if (playerHasResearch) ItemStack.of(Material.LIME_STAINED_GLASS_PANE) else research.item, "${rebarKey("research")}:${research.key}:$playerHasResearch")
            .name(research.name)

        if (playerHasResearch) {
            if (research.cost != null) {
                item.lore(Component.translatable(
                    "rebar.guide.button.research.cost.researched",
                    RebarArgument.of("unlock_cost", UnitFormat.RESEARCH_POINTS.format(research.cost)),
                ))
            }
        } else {
            if (research.cost == null) {
                item.lore(Component.translatable("${research.key.namespace}.researches.${research.key.key}.unlock-instructions"))
            } else {
                val playerPoints = player.researchPoints
                item.lore(Component.translatable(
                    "rebar.guide.button.research.cost."
                            + (if (research.cost > playerPoints) "not-enough" else "enough"),
                    RebarArgument.of("player_points", playerPoints),
                    RebarArgument.of("unlock_cost", UnitFormat.RESEARCH_POINTS.format(research.cost))
                ))
            }
        }

        item.lore(Component.translatable("rebar.guide.button.research.unlocks-title"))

        val shouldCutOff = research.unlocks.size > MAX_UNLOCK_LIST_LINES
        val itemListCount = if (shouldCutOff) {
            MAX_UNLOCK_LIST_LINES - 1
        } else {
            research.unlocks.size
        }

        var i = 0
        for (researchItemKey in research.unlocks) {
            if (i >= itemListCount) {
                break
            }
            i++

            item.lore(Component.translatable(
                "rebar.guide.button.research.unlocks-item",
                RebarArgument.of("item", Component.translatable("${researchItemKey.namespace}.item.${researchItemKey.key}.name"))
            ))
        }

        if (shouldCutOff) {
            item.lore(Component.translatable(
                "rebar.guide.button.research.more-researches",
                RebarArgument.of("amount", research.unlocks.size - i)
            ))
        }

        if (player.guideHints) {
            item.lore(Component.translatable("rebar.guide.button.research.hints."
                    + (if (playerHasResearch) "researched" else "unresearched")
            ))
        }

        item
    } catch (e: Exception) {
        e.printStackTrace()
        ItemStackBuilder.of(Material.BARRIER)
            .name(Component.translatable("rebar.guide.button.fluid.error"))
    }

    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        try {
            if (clickType.isLeftClick) {
                if (research.isResearchedBy(player) || research.cost == null || research.cost > player.researchPoints) {
                    return
                }
                research.addTo(player)
                player.researchPoints -= research.cost
                for (slot in 0 until gui.size) {
                    val item = gui[slot] as? SlotElement.Item ?: continue
                    if (item.item is ResearchButton) {
                        item.item.notifyWindows()
                    }
                }
                player.playGuideSound(RebarConfig.GuideConfig.CLICK_BUTTON_SOUND)
            } else if (clickType.isRightClick) {
                ResearchItemsPage(research).open(player)
                player.playGuideSound(RebarConfig.GuideConfig.CLICK_BUTTON_SOUND)
            } else if (clickType == ClickType.MIDDLE) {
                if (player.hasPermission("rebar.command.research.modify")) {
                    research.addTo(player)
                    notifyWindows()
                    player.playGuideSound(RebarConfig.GuideConfig.CLICK_BUTTON_SOUND)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val MAX_UNLOCK_LIST_LINES = 10
    }
}
