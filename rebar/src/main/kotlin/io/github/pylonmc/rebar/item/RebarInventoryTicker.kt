package io.github.pylonmc.rebar.item

import io.github.pylonmc.rebar.item.base.RebarInventoryTicker
import io.github.pylonmc.rebar.util.isSubclassOf
import org.bukkit.Bukkit

internal class RebarInventoryTicker() : Runnable {
    private var count = 0L
    override fun run() {
        for (player in Bukkit.getOnlinePlayers()) {
            for (item in player.inventory) {
                if (!RebarItem.isRebarItem(item, RebarInventoryTicker::class.java)) {
                    continue
                }

                val rebarItem = RebarItem.fromStack(item)
                if (rebarItem is RebarInventoryTicker && count % rebarItem.tickInterval == 0L) {
                    rebarItem.onTick(player)
                }
            }
        }
        count += 1L
    }
}