package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.advancements.RebarCriteria
import io.github.pylonmc.rebar.advancements.trigger.AdvancementTriggerInstance
import io.github.pylonmc.rebar.registry.RebarRegistry
import org.apache.commons.lang3.reflect.TypeUtils
import org.bukkit.NamespacedKey
import java.lang.reflect.Type

/**
 * Parses the `criteria` of the advancement [advancementKey]:
 *
 * ```yaml
 * criteria:
 *   - criteria1:
 *     - rebar:unlock_on_join
 *     - rebar:unlock_on_item_get:
 *         key: pylon:my-item
 * ```
 *
 * Each criterion is a name mapped to a list of triggers. A trigger is either a bare trigger type key,
 * or a trigger type key mapped to its options. The map form (`criteria1: ...` without the leading `-`)
 * is also accepted, for both criteria and triggers.
 */
class AdvancementCriteriaConfigAdapter(private val advancementKey: NamespacedKey) : ConfigAdapter<List<RebarCriteria>> {

    override val type: Type = TypeUtils.parameterize(List::class.java, RebarCriteria::class.java)

    override fun convert(value: Any): List<RebarCriteria> {
        val criteria = keyedEntries(value).map { (name, triggers) ->
            requireNotNull(triggers) { "Criterion '$name' has no triggers" }
            RebarCriteria(name, parseTriggers(name, triggers))
        }
        val duplicates = criteria.groupBy { it.key }.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "Duplicate criterion names: $duplicates" }
        return criteria
    }

    private fun parseTriggers(criterionName: String, value: Any): List<AdvancementTriggerInstance> {
        val triggers = keyedEntries(value).map { (rawTriggerKey, options) ->
            val triggerKey = parseNamespacedKey(rawTriggerKey, "trigger type")
            val trigger = RebarRegistry.ADVANCEMENT_TRIGGERS[triggerKey] ?: throw IllegalArgumentException(
                "Criterion '$criterionName' uses unknown trigger type $triggerKey"
            )
            trigger.createTriggerInstance(advancementKey, entryOptions(rawTriggerKey, options))
        }
        require(triggers.isNotEmpty()) { "Criterion '$criterionName' has no triggers" }
        return triggers
    }
}
