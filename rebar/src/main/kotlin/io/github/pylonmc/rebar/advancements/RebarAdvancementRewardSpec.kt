package io.github.pylonmc.rebar.advancements

import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.registry.RebarRegistry
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player

data class RebarAdvancementRewardSpec(
    private val rewardTypeKey: NamespacedKey,
    val options: ConfigSection
){
    fun grantReward(player: Player){
        RebarRegistry.ADVANCEMENT_REWARDS[rewardTypeKey]!!.grantReward(player, options)
    }
}
