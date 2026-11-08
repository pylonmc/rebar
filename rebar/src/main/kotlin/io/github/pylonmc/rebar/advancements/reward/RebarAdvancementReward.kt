package io.github.pylonmc.rebar.advancements.reward

import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.registry.RebarRegistry
import org.bukkit.Keyed
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player

abstract class RebarAdvancementReward(private val key: NamespacedKey) : Keyed {
    abstract fun grantReward(player: Player, options: ConfigSection)
    override fun getKey(): NamespacedKey = key

    fun register(){
        RebarRegistry.ADVANCEMENT_REWARDS.register(this)
    }
}