package io.github.pylonmc.rebar.advancements.reward

import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.registry.RebarRegistry
import org.bukkit.Keyed
import org.bukkit.NamespacedKey

abstract class RebarAdvancementReward<RewardInstanceType : AdvancementRewardInstance>(private val key: NamespacedKey) : Keyed {
    /**
     * Called while the advancement [advancementKey] is being parsed, so the [io.github.pylonmc.rebar.advancements.RebarAdvancement]
     * does not exist yet. Read and validate [options] here, throwing if they are invalid, so that mistakes are reported
     * when the advancement is loaded rather than when a player completes it. Pass [advancementKey] on to the
     * [AdvancementRewardInstance] constructor. [options] is empty if the reward was written as a bare key.
     */
    abstract fun createRewardInstance(advancementKey: NamespacedKey, options: ConfigSection): RewardInstanceType

    override fun getKey(): NamespacedKey = key

    fun register(){
        RebarRegistry.ADVANCEMENT_REWARDS.register(this)
    }
}
