package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.advancements.RebarAdvancement
import java.lang.reflect.Type

/**
 * Parses a whole advancement. The section's name is the advancement's key, so this must be read straight
 * from the top level of an advancements file (`config.getOrThrow("pylon:my_advancement", ADVANCEMENT)`).
 *
 * Optional keys may be left out, but any key that is present must be valid; nothing is silently ignored.
 * If `requirements` is left out, every criterion is required (the same default as vanilla).
 */
object AdvancementConfigAdapter : ConfigAdapter<RebarAdvancement> {

    private val REQUIREMENTS = ConfigAdapter.LIST.from(ConfigAdapter.LIST.from(ConfigAdapter.STRING))

    override val type: Type = RebarAdvancement::class.java

    override fun convert(value: Any): RebarAdvancement {
        val section = ConfigAdapter.CONFIG_SECTION.convert(value)
        val advancementKey = parseNamespacedKey(
            section.name ?: throw IllegalArgumentException("Advancement section has no name to use as its key"),
            "advancement"
        )

        val criteria = section.getIfPresent("criteria", AdvancementCriteriaConfigAdapter(advancementKey)) ?: emptyList()
        val criterionNames = criteria.map { it.key }.toSet()
        val requirements = section.getIfPresent("requirements", REQUIREMENTS)
            ?: criteria.map { listOf(it.key) }
        for (group in requirements) {
            require(group.isNotEmpty()) { "Advancement $advancementKey has an empty requirements group" }
            val unknown = group.filter { it !in criterionNames }
            require(unknown.isEmpty()) {
                "Advancement $advancementKey requirements reference unknown criteria $unknown (known criteria: $criterionNames)"
            }
        }

        return RebarAdvancement(
            advancementKey,
            section.getIfPresent("parent", ConfigAdapter.NAMESPACED_KEY),
            section.getIfPresent("display-info", AdvancementDisplayInfoConfigAdapter(advancementKey)),
            section.getIfPresent("rewards", AdvancementRewardsConfigAdapter) ?: emptyList(),
            criteria,
            requirements
        )
    }
}
