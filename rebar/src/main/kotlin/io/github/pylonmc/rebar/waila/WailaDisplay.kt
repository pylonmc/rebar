package io.github.pylonmc.rebar.waila

import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.item.RebarItem
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.format.TextColor

/**
 * The configuration for a WAILA bossbar (the bar shown at the top of your
 * screen when looking at a block).
 */
class WailaDisplay @JvmOverloads constructor(
    var text: Component,
    var color: BossBar.Color = RebarConfig.WailaConfig.DEFAULT_DISPLAY.color,
    var overlay: BossBar.Overlay = RebarConfig.WailaConfig.DEFAULT_DISPLAY.overlay,
    var progress: Float = RebarConfig.WailaConfig.DEFAULT_DISPLAY.progress
) {

    fun color(color: BossBar.Color) = apply { this.color = color }
    fun overlay(overlay: BossBar.Overlay) = apply { this.overlay = overlay }
    fun progress(progress: Float) = apply { this.progress = progress }

    fun separator() = apply {
        text = text.append(seperator)
    }

    fun add(componentLike: ComponentLike) = apply {
        text = text.append(seperator).append(componentLike)
    }

    fun addWithoutSeperator(componentLike: ComponentLike) = apply {
        text = text.append(componentLike)
    }

    companion object {

        val seperator = Component.text(" | ")
            .color(TextColor.fromHexString("#b2b2b2"))

        /**
         * Constructs a new WAILA builder which begins with the given [component].
         */
        @JvmStatic
        fun of(component: Component) = WailaDisplay(component)

        /**
         * Constructs a new WAILA builder which begins with the name of the provided [item].
         */
        @JvmStatic
        fun of(item: RebarItem) = of(Component.translatable("${item.key.namespace}.item.${item.key.key}.name"))

        /**
         * Constructs a new WAILA builder which begins with the name of the provided [block] and
         * has progress equal to the break progress of the block.
         */
        @JvmStatic
        fun of(block: RebarBlock) = of(Component.translatable("${block.key.namespace}.item.${block.key.key}.name"))
            .progress(1.0F - block.breakProgress)
    }
}