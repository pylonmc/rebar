package io.github.pylonmc.rebar.guide.pages.settings

import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.guide.button.PageButton
import io.github.pylonmc.rebar.guide.button.setting.TogglePlayerSettingButton
import io.github.pylonmc.rebar.item.research.Research.Companion.guideHints
import io.github.pylonmc.rebar.item.research.Research.Companion.researchConfetti
import io.github.pylonmc.rebar.item.research.Research.Companion.researchSounds
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.Material

object MainSettingsPage : PlayerSettingsPage(rebarKey("settings")) {

    @JvmStatic
    val wailaSettingsButton = PageButton(Material.SPYGLASS, WailaSettingsPage)

    @JvmStatic
    val blockCullingSettingsButton = PageButton(Material.TINTED_GLASS, BlockCullingSettingsPage)

    @JvmStatic
    val resourcePackSettingsButton = PageButton(Material.PAINTING, ResourcePackSettingsPage)

    @JvmStatic
    val researchConfettiButton = TogglePlayerSettingButton(
        rebarKey("toggle-research-confetti"),
        toggle = { player -> player.researchConfetti = !player.researchConfetti },
        isEnabled = { player -> player.researchConfetti }
    )

    @JvmStatic
    val researchSoundsButton = TogglePlayerSettingButton(
        rebarKey("toggle-research-sounds"),
        toggle = { player -> player.researchSounds = !player.researchSounds },
        isEnabled = { player -> player.researchSounds }
    )

    @JvmStatic
    val guideHintsButton = TogglePlayerSettingButton(
        rebarKey("toggle-guide-hints"),
        toggle = { player -> player.guideHints = !player.guideHints },
        isEnabled = { player -> player.guideHints }
    )

    init {
        if (RebarConfig.WailaConfig.ENABLED) {
            addSetting(wailaSettingsButton)
        }

        if (RebarConfig.CullingEngineConfig.ENABLED) {
            addSetting(blockCullingSettingsButton)
        }

        addSetting(resourcePackSettingsButton)

        if (RebarConfig.RESEARCHES_ENABLED) {
            addSetting(researchConfettiButton)
            addSetting(researchSoundsButton)
        }

        addSetting(guideHintsButton)
    }
}