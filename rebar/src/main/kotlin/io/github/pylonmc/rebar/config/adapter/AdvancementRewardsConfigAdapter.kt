package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.advancements.reward.AdvancementRewardInstance
import io.github.pylonmc.rebar.registry.RebarRegistry
import org.apache.commons.lang3.reflect.TypeUtils
import org.bukkit.NamespacedKey
import java.lang.reflect.Type

/**
 * Parses the `rewards` of the advancement [advancementKey]: reward type keys mapped to their options.
 *
 * ```yaml
 * rewards:
 *   rebar:grant_experience:
 *     amount: 10
 * ```
 *
 * The list form (`- rebar:grant_experience: ...`) is also accepted.
 */
class AdvancementRewardsConfigAdapter(private val advancementKey: NamespacedKey) :
    ConfigAdapter<List<AdvancementRewardInstance>> {

    override val type: Type = TypeUtils.parameterize(List::class.java, AdvancementRewardInstance::class.java)

    override fun convert(value: Any): List<AdvancementRewardInstance> =
        AdvancementCriteriaConfigAdapter.keyedEntries(value).map { (rawRewardKey, options) ->
            val rewardKey = NamespacedKey.fromString(rawRewardKey)
                ?: throw IllegalArgumentException("'$rawRewardKey' is not a valid reward type key")
            val reward = RebarRegistry.ADVANCEMENT_REWARDS[rewardKey]
                ?: throw IllegalArgumentException("Unknown advancement reward type $rewardKey")
            reward.createRewardInstance(
                advancementKey,
                AdvancementCriteriaConfigAdapter.entryOptions(rawRewardKey, options)
            )
        }
}
