package io.github.pylonmc.rebar.guide.pages.info

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.guide.button.GuideButton
import io.github.pylonmc.rebar.guide.button.PageButton
import io.github.pylonmc.rebar.guide.pages.base.SimpleStaticGuidePage
import io.github.pylonmc.rebar.guide.pages.info.sub.AdministratorsPage
import io.github.pylonmc.rebar.guide.pages.info.sub.RebarInfoPage
import io.github.pylonmc.rebar.guide.pages.info.sub.ResearchingInfoPage
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.util.rebarKey
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import xyz.xenondevs.invui.Click

object InfoPage : SimpleStaticGuidePage(rebarKey("info")) {
    @JvmStatic
    val administratorsPage = AdministratorsPage

    @JvmStatic
    val contributorsPage = ContributorsPage

    @JvmStatic
    val discordButton = object : GuideButton() {
        override fun getItemProvider(viewer: Player) = ItemStackBuilder.guide(Material.WRITABLE_BOOK, Rebar, "info.discord")

        override fun handleClick(clickType: ClickType, player: Player, click: Click) {
            player.sendMessage(Component.translatable("rebar.guide.button.info.discord.message")
                .clickEvent(ClickEvent.openUrl("https://discord.gg/4tMAnBAacW")))
        }

        override fun priority() = 0.0
    }

    @JvmStatic
    val rebarInfoPageButton = PageButton(Rebar.material, RebarInfoPage)

    @JvmStatic
    val researchingInfoPageButton = PageButton(Material.LECTERN, ResearchingInfoPage)

    init {
        addPage(Material.BARRIER, administratorsPage)
        addPage(Material.PLAYER_HEAD, contributorsPage)
        if (RebarConfig.GuideConfig.DISCORD_BUTTON) {
            addButton(discordButton)
        }
        addButton(rebarInfoPageButton)
        if (RebarConfig.ResearchConfig.ENABLED) {
            addButton(researchingInfoPageButton)
        }
    }
}