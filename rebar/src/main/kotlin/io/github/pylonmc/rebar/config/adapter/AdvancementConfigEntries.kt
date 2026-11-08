package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.config.ConfigSection
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemoryConfiguration

/*
 * Shared parsing helpers for the advancement config format.
 *
 * Criteria, triggers and rewards are all written as "keyed entries", which may take either shape:
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

/**
 * Converts a keyed-entry collection (map/section, or list of bare keys / single-entry maps) into
 * ordered `(key, value)` pairs. The value is null for a bare key or a key with nothing after it.
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

internal fun parseNamespacedKey(raw: String, what: String): NamespacedKey =
    NamespacedKey.fromString(raw) ?: throw IllegalArgumentException("'$raw' is not a valid $what key")

/**
 * Like [ConfigSection.get], but only returns null when the key is absent. A value that is present but
 * malformed throws (with the key path) instead of being silently discarded.
 */
internal fun <T> ConfigSection.getIfPresent(key: String, adapter: ConfigAdapter<T>): T? =
    if (has(key)) getOrThrow(key, adapter) else null
