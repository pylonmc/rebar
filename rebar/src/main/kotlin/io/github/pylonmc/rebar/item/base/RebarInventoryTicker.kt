package io.github.pylonmc.rebar.item.base

import io.github.pylonmc.rebar.config.RebarConfig
import org.bukkit.entity.Player

/**
 * An item should implement this interface to tick when a player has the item in their inventory
 */
interface RebarInventoryTicker {
    /**
     * Called when the item is detected in the player's inventory.
     * will be called at [baseTickInterval] * [RebarConfig.inventoryTickerBaseRate
     * @param player The player whose inventory the item was in
     */
    fun onTick(player: Player)

    /** Determines the rate at which the [onTick] method will be called.
     * [onTick] will be called at [baseTickInterval] * [RebarConfig.INVENTORY_TICKER_BASE_RATE] */
    val baseTickInterval: Long
}