package io.github.pylonmc.rebar.guide.button

import org.bukkit.entity.Player
import xyz.xenondevs.invui.item.AbstractItem

abstract class GuideButton : AbstractItem() {

    open fun shouldDisplay(player: Player): Boolean = true

    open fun priority(): Double = 1.0

}