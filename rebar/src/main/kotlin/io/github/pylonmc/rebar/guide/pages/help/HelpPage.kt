package io.github.pylonmc.rebar.guide.pages.help

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.guide.button.PageButton
import io.github.pylonmc.rebar.guide.pages.base.SimpleStaticGuidePage
import io.github.pylonmc.rebar.guide.pages.help.sub.AdministratorsPage
import io.github.pylonmc.rebar.guide.pages.help.sub.RebarHelpPage
import io.github.pylonmc.rebar.guide.pages.help.sub.ResearchingHelpPage
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.Material

object HelpPage : SimpleStaticGuidePage(rebarKey("info")) {
    @JvmStatic
    val administratorsPage = AdministratorsPage

    @JvmStatic
    val contributorsPage = ContributorsPage

    @JvmStatic
    val rebarHelpPageButton = PageButton(Rebar.material, RebarHelpPage)

    @JvmStatic
    val researchingHelpPageButton = PageButton(Material.LECTERN, ResearchingHelpPage)

    init {
        addPage(Material.BARRIER, administratorsPage)
        addPage(Material.PLAYER_HEAD, contributorsPage)
        addButton(rebarHelpPageButton)
        if (RebarConfig.ResearchConfig.ENABLED) {
            addButton(researchingHelpPageButton)
        }
    }
}