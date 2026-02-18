package io.github.pylonmc.rebar.guide.pages.settings

import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.culling.BlockCullingEngine.hasBlockCulling
import io.github.pylonmc.rebar.culling.BlockCullingEngine.playerCullingConfig
import io.github.pylonmc.rebar.guide.button.PageButton
import io.github.pylonmc.rebar.guide.button.setting.CyclePlayerSettingButton
import io.github.pylonmc.rebar.guide.button.setting.TogglePlayerSettingButton
import io.github.pylonmc.rebar.i18n.RebarArgument
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.util.rebarKey
import io.papermc.paper.command.brigadier.argument.ArgumentTypes.player
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import xyz.xenondevs.invui.Click
import xyz.xenondevs.invui.item.ItemProvider

object BlockCullingSettingsPage : PlayerSettingsPage(rebarKey("block_culling_settings")) {
    init {
        addSetting(
            TogglePlayerSettingButton(
                rebarKey("toggle-block-culling"),
                toggle = { player ->
                    player.hasBlockCulling = !player.hasBlockCulling
                    buttons.forEach { button -> button.notifyWindows() }
                 },
                isEnabled = { player -> player.hasBlockCulling }
            )
        )

        val ids = listOf("custom", "disabled").plus(
            RebarConfig.CullingEngineConfig.CULLING_PRESETS
                .toList().sortedBy { it.second.index }.map { it.first }
        )

        addSetting(
            CyclePlayerSettingButton(
                rebarKey("cycle-culling-preset"),
                ids,
                identifier = { presetId -> presetId },
                getter = { player ->
                    if (!player.hasBlockCulling) {
                        "disabled"
                    } else {
                        val playerConfig = player.playerCullingConfig
                        val currentPreset = RebarConfig.CullingEngineConfig.CULLING_PRESETS.values.firstOrNull { it.matches(playerConfig) }
                        currentPreset?.id ?: "custom"
                    }
                },
                setter = { player, presetId ->
                    val nextPresetId = when(presetId) {
                        // Player cannot cycle to these values, push to the first actual preset
                        "disabled", "custom" -> ids[2]
                        else -> presetId
                    }
                    val preset = RebarConfig.CullingEngineConfig.CULLING_PRESETS[nextPresetId] ?: throw IllegalStateException("Invalid preset id: $nextPresetId")
                    player.playerCullingConfig = preset.toPlayerConfig()
                },
                decorator = { _, presetId ->
                    val material = when(presetId) {
                        "disabled" -> Material.LIGHT_GRAY_CONCRETE
                        "custom" -> Material.PURPLE_CONCRETE
                        else -> RebarConfig.CullingEngineConfig.CULLING_PRESETS.getOrDefault(presetId, RebarConfig.CullingEngineConfig.DEFAULT_CULLING_PRESET).material
                    }
                    ItemStackBuilder.of(material)
                        .addCustomModelDataString("culling_preset=$presetId")
                        .build()
                },
                placeholderProvider = { player, _ ->
                    if (!player.hasBlockCulling) {
                        return@CyclePlayerSettingButton mutableListOf()
                    }

                    val config = player.playerCullingConfig
                    mutableListOf(
                        RebarArgument.of("hiddenInterval", config.hiddenInterval),
                        RebarArgument.of("visibleInterval", config.visibleInterval),
                        RebarArgument.of("alwaysShowRadius", config.alwaysShowRadius),
                        RebarArgument.of("cullRadius", config.cullRadius),
                        RebarArgument.of("maxOccludingCount", config.maxOccludingCount)
                    )
                }
            )
        )

        if (!RebarConfig.CullingEngineConfig.PRESETS_ONLY) {
            addSetting(object : PageButton(Material.COMPARATOR, AdvancedBlockCullingSettingsPage) {
                override fun getItemProvider(viewer: Player): ItemProvider {
                    if (!viewer.hasBlockCulling) {
                        return ItemProvider.EMPTY
                    }
                    return super.getItemProvider(viewer)
                }

                override fun handleClick(clickType: ClickType, player: Player, click: Click) {
                    if (player.hasBlockCulling) {
                        super.handleClick(clickType, player, click)
                    }
                }

                override fun priority() = 1.0
            })
        }
    }
}