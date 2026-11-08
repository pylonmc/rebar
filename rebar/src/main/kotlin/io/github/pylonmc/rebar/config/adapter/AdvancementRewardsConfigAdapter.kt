package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.advancements.RebarAdvancementRewardSpec
import io.github.pylonmc.rebar.registry.RebarRegistry
import org.apache.commons.lang3.reflect.TypeUtils
import org.bukkit.NamespacedKey
import java.lang.reflect.Type

/**
 * Parses the `rewards` of an advancement: reward type keys mapped to their options.
 *
 * ```yaml
 * rewards:
 *   rebar:grant_experience:
 *     amount: 10
 * ```
 *
 * The list form (`- rebar:grant_experience: ...`) is also accepted. Reward types are resolved here, so an
 * unknown type fails when the advancement is loaded rather than when it is completed.
 */
object AdvancementRewardsConfigAdapter : ConfigAdapter<List<RebarAdvancementRewardSpec>> {

    override val type: Type = TypeUtils.parameterize(List::class.java, RebarAdvancementRewardSpec::class.java)

    override fun convert(value: Any): List<RebarAdvancementRewardSpec> =
        keyedEntries(value).map { (rawRewardKey, options) ->
            val rewardKey = NamespacedKey.fromString(rawRewardKey)
                ?: throw IllegalArgumentException("'$rawRewardKey' is not a valid reward type key")
            val reward = RebarRegistry.ADVANCEMENT_REWARDS[rewardKey]
                ?: throw IllegalArgumentException("Unknown advancement reward type $rewardKey")
            RebarAdvancementRewardSpec(reward, entryOptions(rawRewardKey, options))
        }
}
