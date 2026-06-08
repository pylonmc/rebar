package io.github.pylonmc.rebar.guide.button

import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.content.guide.RebarGuide
import io.github.pylonmc.rebar.guide.pages.base.PagedGuidePage
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.content.guide.RebarGuide.Companion.guideHints
import io.github.pylonmc.rebar.content.guide.RebarGuide.Companion.playGuideSound
import io.github.pylonmc.rebar.util.rebarKey
import io.papermc.paper.datacomponent.DataComponentTypes
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import xyz.xenondevs.invui.Click
import xyz.xenondevs.invui.item.AbstractItem

/**
 * Represents the back button in the guide.
 */
@Suppress("UnstableApiUsage")
class BackButton : AbstractItem() {

    override fun getItemProvider(player: Player) =
        ItemStackBuilder.gui(Material.ENCHANTED_BOOK, rebarKey("guide_back"))
            .set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false)
            .name(Component.translatable("rebar.guide.button.back.name")).apply {
                if (player.guideHints) {
                    lore(Component.translatable("rebar.guide.button.back.hints"))
                }
            }

    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        val history = RebarGuide.history.getOrPut(player.uniqueId) { mutableListOf() }
        if (clickType.isShiftClick) {
            history.clear()
            RebarGuide.rootPage.open(player)
        } else if (history.size >= 2) {
            val currentPage = history.removeLast() // remove the current page & forget the page number
            if (currentPage is PagedGuidePage) {
                currentPage.resetCurrentPage(player)
            }

            history.removeLast().open(player)
        }
        player.playGuideSound(RebarConfig.GuideConfig.CLICK_BUTTON_SOUND)
    }
}