package io.github.pylonmc.rebar.guide.pages.info

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.guide.button.PageButton
import io.github.pylonmc.rebar.guide.pages.base.SimpleStaticGuidePage
import io.github.pylonmc.rebar.guide.pages.info.sub.AdministratorsPage
import io.github.pylonmc.rebar.guide.pages.info.sub.RebarInfoPage
import io.github.pylonmc.rebar.guide.pages.info.sub.ResearchingInfoPage
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.Material

object InfoPage : SimpleStaticGuidePage(rebarKey("info")) {
    @JvmStatic
    val administratorsPage = AdministratorsPage

    @JvmStatic
    val contributorsPage = ContributorsPage

    @JvmStatic
    val rebarInfoPageButton = PageButton(Rebar.material, RebarInfoPage)

    @JvmStatic
    val researchingInfoPageButton = PageButton(Material.LECTERN, ResearchingInfoPage)

    init {
        addPage(Material.BARRIER, administratorsPage)
        addPage(Material.PLAYER_HEAD, contributorsPage)
        addButton(rebarInfoPageButton)
        if (RebarConfig.ResearchConfig.ENABLED) {
            addButton(researchingInfoPageButton)
        }
    }
}