package io.github.pylonmc.rebar.advancements.reward.base

import io.github.pylonmc.rebar.advancements.reward.RebarAdvancementReward
import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import org.bukkit.NamespacedKey

class GrantExperienceReward(key: NamespacedKey) : RebarAdvancementReward<GrantExperienceRewardInstance>(key) {
    override fun createRewardInstance(
        advancementKey: NamespacedKey,
        options: ConfigSection
    ): GrantExperienceRewardInstance {
        return GrantExperienceRewardInstance(key, advancementKey, options.getOrThrow("amount", ConfigAdapter.INTEGER))
    }
}