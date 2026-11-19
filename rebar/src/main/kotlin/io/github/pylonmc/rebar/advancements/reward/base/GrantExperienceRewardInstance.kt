package io.github.pylonmc.rebar.advancements.reward.base

import io.github.pylonmc.rebar.advancements.reward.AdvancementRewardInstance
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player

class GrantExperienceRewardInstance(
    rewardKey: NamespacedKey,
    advancementKey: NamespacedKey,
    val amount: Int
) : AdvancementRewardInstance(rewardKey, advancementKey) {
    override fun grant(player: Player) {
        player.giveExp(amount)
    }
}