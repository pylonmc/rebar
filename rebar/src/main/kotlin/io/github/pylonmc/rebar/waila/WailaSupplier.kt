package io.github.pylonmc.rebar.waila

import org.bukkit.entity.Player

@FunctionalInterface
fun interface WailaSupplier {
    fun getWaila(player: Player): WailaDisplay?
}