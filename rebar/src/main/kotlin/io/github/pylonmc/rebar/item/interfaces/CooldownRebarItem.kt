package io.github.pylonmc.rebar.item.interfaces

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Implemented by other Rebar interfaces that may be affected by cooldowns.
 */
interface CooldownRebarItem {

    val stack: ItemStack

    @Suppress("INAPPLICABLE_JVM_NAME") // tfw suppressing errors
    @get:JvmName("respectCooldown")
    val respectCooldown: Boolean
        get() = true

    fun hasCooldown(player: Player) = respectCooldown && player.getCooldown(stack) > 0

}