package io.github.pylonmc.rebar.advancements.reward.base

import io.github.pylonmc.rebar.advancements.reward.AdvancementReward
import io.github.pylonmc.rebar.config.ConfigSection
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player

class AdvancementRewardItem(key: NamespacedKey) : AdvancementReward(key) {
    override fun grantReward(player: Player, options: ConfigSection) {
        TODO("Not yet implemented")
    }
}