package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.advancements.RebarCriteria
import io.github.pylonmc.rebar.advancements.trigger.AdvancementTriggerInstance
import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.registry.RebarRegistry
import org.apache.commons.lang3.reflect.TypeUtils
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemoryConfiguration
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
            val triggerKey = NamespacedKey.fromString(rawTriggerKey)
                ?: throw IllegalArgumentException("'$rawTriggerKey' is not a valid trigger type key")
            val trigger = RebarRegistry.ADVANCEMENT_TRIGGERS[triggerKey] ?: throw IllegalArgumentException(
                "Criterion '$criterionName' uses unknown trigger type $triggerKey"
            )
            trigger.createTriggerInstance(advancementKey, criterionName, entryOptions(rawTriggerKey, options))
        }


    /**
     * Converts a keyed-entry collection (map/section, or list of bare keys / single-entry maps) into
     * ordered `(key, value)` pairs. The value is null for a bare key or a key with nothing after it.
     *
     * Criteria, triggers and rewards are all written as keyed entries, which may take either shape:
     *
     * ```yaml
     * # map form
     * rewards:
     *   rebar:grant_experience:
     *     amount: 10
     *
     * # list form - each element is a bare key, or a single `key: value` entry
     * - rebar:unlock_on_join
     * - rebar:unlock_on_item_get:
     *     key: pylon:my-item
     * ```
     */
    internal fun keyedEntries(value: Any): List<Pair<String, Any?>> = when (value) {
        is ConfigurationSection -> value.getKeys(false).map { it to value.get(it) }
        is Map<*, *> -> value.map { (k, v) -> k.toString() to v }
        is List<*> -> value.map { element ->
            when (element) {
                is String -> element to null
                is Map<*, *> -> {
                    require(element.size == 1) {
                        "Expected a single 'key: value' entry but found ${element.size} keys: ${element.keys}"
                    }
                    val (k, v) = element.entries.single()
                    k.toString() to v
                }
                is ConfigurationSection -> {
                    val keys = element.getKeys(false)
                    require(keys.size == 1) {
                        "Expected a single 'key: value' entry but found ${keys.size} keys: $keys"
                    }
                    keys.single() to element.get(keys.single())
                }
                else -> throw IllegalArgumentException(
                    "Expected a key or a single 'key: value' entry, but got: $element"
                )
            }
        }
        else -> throw IllegalArgumentException("Expected a list or section, but got: ${value::class.java.name}")
    }

    /**
     * Converts the value of a keyed entry into an options section. A missing value becomes an empty section.
     */
    internal fun entryOptions(entryKey: String, value: Any?): ConfigSection = when (value) {
        null -> ConfigSection.from(entryKey, MemoryConfiguration())
        is ConfigurationSection, is Map<*, *> -> ConfigAdapter.CONFIG_SECTION.convert(value)
        else -> throw IllegalArgumentException("Expected options for '$entryKey' to be a section, but got: $value")
    }
}