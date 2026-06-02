package io.github.pylonmc.rebar.waila

import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.item.RebarItem
import net.kyori.adventure.text.Component

/**
 * A simple utility class for building WAILA displays.
 */
class WailaDisplayBuilder private constructor(text: Component) {

    var display = WailaDisplay(text)

}