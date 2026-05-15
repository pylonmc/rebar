package io.github.pylonmc.rebar.item.base

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.item.RebarItem
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitTask
import org.jetbrains.annotations.MustBeInvokedByOverriders
import java.util.UUID

interface RebarInventoryEffectItem : RebarInventoryTicker {
    @MustBeInvokedByOverriders
    override fun onTick(player: Player) {
        tasks.putIfAbsent(itemKey, HashMap())
        tasks[itemKey]!![player.uniqueId]?.cancel()
        if (!player.persistentDataContainer.has(itemKey)) {
            player.persistentDataContainer.set(itemKey, PersistentDataType.BOOLEAN, true)
            onAddedToInventory(player)
        }
        tasks[itemKey]!![player.uniqueId] = Bukkit.getScheduler().runTaskLater(Rebar.javaPlugin, Runnable {
            player.persistentDataContainer.remove(itemKey)
            onRemovedFromInventory(player)
        }, baseTickInterval * RebarConfig.INVENTORY_TICKER_BASE_RATE + 1)
    }

    /**
     * Remove the effect from the player. Best-effort removal therefore is no guarantee that the player is still connected or that the
     * [RebarItem.stack] is up to date with the actual ItemStack when it runs.
     */
    @MustBeInvokedByOverriders
    fun onRemovedFromInventory(player: Player) {
        player.persistentDataContainer.remove(itemKey)
    }

    /**
     * Apply the effect of this item onto the player
     */
    @MustBeInvokedByOverriders
    fun onAddedToInventory(player: Player) {
        player.persistentDataContainer.set(itemKey, PersistentDataType.BOOLEAN, true)
    }

    val itemKey: NamespacedKey
        get() = NamespacedKey((this as RebarItem).key.namespace, key.key + "_effect")

    companion object {
        private val tasks = HashMap<NamespacedKey, HashMap<UUID, BukkitTask>>()
    }
}