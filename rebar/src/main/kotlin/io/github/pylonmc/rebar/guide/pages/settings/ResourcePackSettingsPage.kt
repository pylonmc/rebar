package io.github.pylonmc.rebar.guide.pages.settings

import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.guide.button.setting.TogglePlayerSettingButton
import io.github.pylonmc.rebar.resourcepack.armor.ArmorTextureEngine.hasCustomArmorTextures
import io.github.pylonmc.rebar.resourcepack.block.BlockTextureEngine.hasCustomBlockTextures
import io.github.pylonmc.rebar.util.rebarKey

object ResourcePackSettingsPage : PlayerSettingsPage(rebarKey("resource_pack_settings")) {

    init {
        if (RebarConfig.ArmorTextureConfig.ENABLED && !RebarConfig.ArmorTextureConfig.FORCED) {
            addSetting(
                TogglePlayerSettingButton(
                    rebarKey("toggle-armor-textures"),
                    toggle = { player -> player.hasCustomArmorTextures = !player.hasCustomArmorTextures },
                    isEnabled = { player -> player.hasCustomArmorTextures },
                )
            )
        }

        if (RebarConfig.CullingEngineConfig.ENABLED && RebarConfig.BlockTextureConfig.ENABLED && !RebarConfig.BlockTextureConfig.FORCED) {
            addSetting(
                TogglePlayerSettingButton(
                    rebarKey("toggle-block-textures"),
                    toggle = { player -> player.hasCustomBlockTextures = !player.hasCustomBlockTextures },
                    isEnabled = { player -> player.hasCustomBlockTextures },
                )
            )
        }
    }
}