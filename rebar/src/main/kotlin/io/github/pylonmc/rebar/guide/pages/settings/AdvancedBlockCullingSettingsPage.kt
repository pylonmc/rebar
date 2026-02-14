package io.github.pylonmc.rebar.guide.pages.settings

import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.culling.BlockCullingEngine.hasBlockCulling
import io.github.pylonmc.rebar.culling.BlockCullingEngine.playerCullingConfig
import io.github.pylonmc.rebar.guide.button.setting.NumericPlayerSettingButton
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.Material

object AdvancedBlockCullingSettingsPage : PlayerSettingsPage(rebarKey("advanced_block_culling_settings")) {
    init {
        addSetting(NumericPlayerSettingButton(
            key = rebarKey("culling-update-interval"),
            min = RebarConfig.CullingEngineConfig.UPDATE_INTERVAL_LIMIT.first,
            max = RebarConfig.CullingEngineConfig.UPDATE_INTERVAL_LIMIT.last,
            step = 1,
            shiftStep = 5,
            type = { number -> number.toInt() },
            getter = { player -> player.playerCullingConfig.updateInterval },
            setter = { player, value ->
                val config = player.playerCullingConfig
                config.updateInterval = value
                player.playerCullingConfig = config
            },
            decorator = { player, _ ->
                if (player.hasBlockCulling) {
                    ItemStackBuilder.of(Material.CLOCK).build()
                } else {
                    ItemStackBuilder.of(Material.LIGHT_GRAY_CONCRETE).build()
                }
            }
        ))
        addSetting(NumericPlayerSettingButton(
            key = rebarKey("culling-hidden-interval"),
            min = RebarConfig.CullingEngineConfig.HIDDEN_INTERVAL_LIMIT.first,
            max = RebarConfig.CullingEngineConfig.HIDDEN_INTERVAL_LIMIT.last,
            step = 1,
            shiftStep = 5,
            type = { number -> number.toInt() },
            getter = { player -> player.playerCullingConfig.hiddenInterval },
            setter = { player, value ->
                val config = player.playerCullingConfig
                config.hiddenInterval = value
                player.playerCullingConfig = config
            },
            decorator = { player, _ ->
                if (player.hasBlockCulling) {
                    ItemStackBuilder.of(Material.ENDER_PEARL).build()
                } else {
                    ItemStackBuilder.of(Material.LIGHT_GRAY_CONCRETE).build()
                }
            }
        ))
        addSetting(NumericPlayerSettingButton(
            key = rebarKey("culling-visible-interval"),
            min = RebarConfig.CullingEngineConfig.VISIBLE_INTERVAL_LIMIT.first,
            max = RebarConfig.CullingEngineConfig.VISIBLE_INTERVAL_LIMIT.last,
            step = 1,
            shiftStep = 5,
            type = { number -> number.toInt() },
            getter = { player -> player.playerCullingConfig.visibleInterval },
            setter = { player, value ->
                val config = player.playerCullingConfig
                config.visibleInterval = value
                player.playerCullingConfig = config
            },
            decorator = { player, _ ->
                if (player.hasBlockCulling) {
                    ItemStackBuilder.of(Material.ENDER_EYE).build()
                } else {
                    ItemStackBuilder.of(Material.LIGHT_GRAY_CONCRETE).build()
                }
            }
        ))
        addSetting(NumericPlayerSettingButton(
            key = rebarKey("culling-always-show-radius"),
            min = RebarConfig.CullingEngineConfig.ALWAYS_SHOW_RADIUS_LIMIT.first,
            max = RebarConfig.CullingEngineConfig.ALWAYS_SHOW_RADIUS_LIMIT.last,
            step = 1,
            shiftStep = 5,
            type = { number -> number.toInt() },
            getter = { player -> player.playerCullingConfig.alwaysShowRadius },
            setter = { player, value ->
                val config = player.playerCullingConfig
                config.alwaysShowRadius = value
                player.playerCullingConfig = config
            },
            decorator = { player, _ ->
                if (player.hasBlockCulling) {
                    ItemStackBuilder.of(Material.LANTERN).build()
                } else {
                    ItemStackBuilder.of(Material.LIGHT_GRAY_CONCRETE).build()
                }
            }
        ))
        addSetting(NumericPlayerSettingButton(
            key = rebarKey("culling-cull-radius"),
            min = RebarConfig.CullingEngineConfig.CULL_RADIUS_LIMIT.first,
            max = RebarConfig.CullingEngineConfig.CULL_RADIUS_LIMIT.last,
            step = 1,
            shiftStep = 5,
            type = { number -> number.toInt() },
            getter = { player -> player.playerCullingConfig.cullRadius },
            setter = { player, value ->
                val config = player.playerCullingConfig
                config.cullRadius = value
                player.playerCullingConfig = config
            },
            decorator = { player, _ ->
                if (player.hasBlockCulling) {
                    ItemStackBuilder.of(Material.COBWEB).build()
                } else {
                    ItemStackBuilder.of(Material.LIGHT_GRAY_CONCRETE).build()
                }
            }
        ))
        addSetting(NumericPlayerSettingButton(
            key = rebarKey("culling-max-occluding-count"),
            min = RebarConfig.CullingEngineConfig.MAX_OCCLUDING_COUNT_LIMIT.first,
            max = RebarConfig.CullingEngineConfig.MAX_OCCLUDING_COUNT_LIMIT.last,
            step = 1,
            shiftStep = 5,
            type = { number -> number.toInt() },
            getter = { player -> player.playerCullingConfig.maxOccludingCount },
            setter = { player, value ->
                val config = player.playerCullingConfig
                config.maxOccludingCount = value
                player.playerCullingConfig = config
            },
            decorator = { player, _ ->
                if (player.hasBlockCulling) {
                    ItemStackBuilder.of(Material.HONEYCOMB).build()
                } else {
                    ItemStackBuilder.of(Material.LIGHT_GRAY_CONCRETE).build()
                }
            }
        ))
    }
}