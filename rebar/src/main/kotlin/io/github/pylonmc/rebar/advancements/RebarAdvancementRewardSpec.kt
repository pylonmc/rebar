package io.github.pylonmc.rebar.advancements

import io.github.pylonmc.rebar.advancements.reward.RebarAdvancementReward
import io.github.pylonmc.rebar.config.ConfigSection
import org.bukkit.entity.Player

class RebarAdvancementRewardSpec(
    val reward: RebarAdvancementReward,
    val options: ConfigSection
) {
    fun grantReward(player: Player) {
        reward.grantReward(player, options)
    }
}
