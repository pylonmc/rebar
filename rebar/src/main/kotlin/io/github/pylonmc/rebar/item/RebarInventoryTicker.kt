package io.github.pylonmc.rebar.item

import io.github.pylonmc.rebar.item.base.RebarInventoryTicker
import org.bukkit.Bukkit

internal class RebarInventoryTicker : Runnable {
    private var count = 0L
    override fun run() {
        for (player in Bukkit.getOnlinePlayers()) {
            for (item in player.inventory) {
                val rebarItem = RebarItem.fromStack(item, RebarInventoryTicker::class.java)
                if (rebarItem is RebarInventoryTicker && count % rebarItem.baseTickInterval == 0L) {
                    rebarItem.onTick(player)
                }
            }
        }
        count += 1L
    }
}