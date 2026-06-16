package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.waila.WailaDisplay
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component

object WailaDisplayConfigAdapter : ConfigAdapter<WailaDisplay> {
    override val type = WailaDisplay::class.java

    override fun convert(value: Any): WailaDisplay {
        val section = ConfigAdapter.CONFIG_SECTION.convert(value)
        return WailaDisplay(
            text = Component.translatable(section.getOrThrow("text", ConfigAdapter.STRING)),
            color = section.getOrThrow("color", ConfigAdapter.ENUM.from<BossBar.Color>()),
            overlay = section.getOrThrow("overlay", ConfigAdapter.ENUM.from<BossBar.Overlay>()),
            progress = section.getOrThrow("progress", ConfigAdapter.FLOAT)
        )
    }
}