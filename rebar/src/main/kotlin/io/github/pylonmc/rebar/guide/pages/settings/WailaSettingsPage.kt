package io.github.pylonmc.rebar.guide.pages.settings

import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.guide.button.setting.CyclePlayerSettingButton
import io.github.pylonmc.rebar.guide.button.setting.TogglePlayerSettingButton
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.waila.Waila.Companion.wailaConfig
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.bossbar.BossBar
import org.bukkit.Material
import org.bukkit.NamespacedKey

object WailaSettingsPage : PlayerSettingsPage(rebarKey("waila_settings")) {
    init {
        addSetting(
            TogglePlayerSettingButton(
                rebarKey("toggle-waila"),
                toggle = { player -> player.wailaConfig.enabled = !player.wailaConfig.enabled },
                isEnabled = { player -> player.wailaConfig.enabled }
            ))
        addSetting(
            TogglePlayerSettingButton(
                rebarKey("toggle-vanilla-waila"),
                toggle = { player ->
                    player.wailaConfig.vanillaWailaEnabled = !player.wailaConfig.vanillaWailaEnabled
                },
                isEnabled = { player -> player.wailaConfig.vanillaWailaEnabled }
            ))
        if (RebarConfig.WailaConfig.ENABLED_TYPES.size > 1) {
            addSetting(
                CyclePlayerSettingButton(
                    rebarKey("cycle-waila-type"),
                    RebarConfig.WailaConfig.ENABLED_TYPES,
                    identifier = { type -> type.name.lowercase() },
                    getter = { player -> player.wailaConfig.type },
                    setter = { player, type -> player.wailaConfig.type = type },
                    decorator = { player, type ->
                        ItemStackBuilder.Companion.of(Material.PAPER)
                            .addCustomModelDataString("waila_type=${type.name.lowercase()}")
                            .build()
                    }
                ))
        }
        if (RebarConfig.WailaConfig.ALLOWED_BOSS_BAR_COLORS.size > 1) {
            addSetting(
                CyclePlayerSettingButton(
                    rebarKey("cycle-waila-boss-bar-color"),
                    RebarConfig.WailaConfig.ALLOWED_BOSS_BAR_COLORS.toList(),
                    identifier = { color -> color.name.lowercase() },
                    getter = { player -> player.wailaConfig.bossbarColor },
                    setter = { player, color -> player.wailaConfig.bossbarColor = color },
                    decorator = { player, color ->
                        ItemStackBuilder.Companion.of(RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM).get(
                            NamespacedKey.minecraft(color.name.lowercase() + "_wool"))!!)
                            .addCustomModelDataString("waila_boss_bar_color=${color.name.lowercase()}")
                            .build()
                    }
                )
            )
        }
        if (RebarConfig.WailaConfig.ALLOWED_BOSS_BAR_OVERLAYS.size > 1) {
            addSetting(
                CyclePlayerSettingButton(
                    rebarKey("cycle-waila-boss-bar-overlay"),
                    RebarConfig.WailaConfig.ALLOWED_BOSS_BAR_OVERLAYS.toList(),
                    identifier = { overlay -> overlay.name.lowercase() },
                    getter = { player -> player.wailaConfig.bossbarOverlay },
                    setter = { player, overlay -> player.wailaConfig.bossbarOverlay = overlay },
                    decorator = { player, overlay ->
                        ItemStackBuilder.Companion.of(Material.NOTE_BLOCK)
                            .addCustomModelDataString("waila_boss_bar_overlay=${overlay.name.lowercase()}")
                            .build()
                    }
                )
            )
        }
    }
}