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
 * Each criterion is a name mapped to a list of triggers, and is awarded when any one of its triggers fires (use
 * `requirements` to require several criteria). A trigger is either a bare trigger type key, or a trigger type key
 * mapped to its options. The map form (`criteria1: ...` without the leading `-`) is also accepted, for both criteria
 * and triggers.
 *
 * A criterion with no triggers can only be awarded through code or `/advancement grant`, which is how an
 * advancement that is only unlocked by a plugin is written:
 *
 * ```yaml
 * criteria:
 *   - unlocked_by_plugin
 * ```
 *
 * (In map form, write `unlocked_by_plugin: []`; Bukkit drops map keys with no value.)
 */
class AdvancementCriteriaConfigAdapter(private val advancementKey: NamespacedKey) : ConfigAdapter<List<RebarCriteria>> {

    override val type: Type = TypeUtils.parameterize(List::class.java, RebarCriteria::class.java)

    // Duplicate names and empty criteria lists are rejected by RebarAdvancement itself
    override fun convert(value: Any): List<RebarCriteria> =
        keyedEntries(value).map { (name, triggers) ->
            RebarCriteria(name, if (triggers == null) emptyList() else parseTriggers(name, triggers))
        }

    private fun parseTriggers(criterionName: String, value: Any): List<AdvancementTriggerInstance> =
        keyedEntries(value).map { (rawTriggerKey, options) ->
            val triggerKey = parseNamespacedKey(rawTriggerKey, "trigger type")
            val trigger = RebarRegistry.ADVANCEMENT_TRIGGERS[triggerKey] ?: throw IllegalArgumentException(
                "Criterion '$criterionName' uses unknown trigger type $triggerKey"
            )
            trigger.createTriggerInstance(advancementKey, criterionName, entryOptions(rawTriggerKey, options))
        }
}
