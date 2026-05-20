package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.advancements.*
import io.github.pylonmc.rebar.registry.RebarRegistry
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import java.lang.reflect.Type

object AdvancementConfigAdapter : ConfigAdapter<RebarAdvancement> {
    override val type: Type
        get() = RebarAdvancement::class.java

    override fun convert(key: String?, value: Any): RebarAdvancement {
        val section = ConfigAdapter.CONFIG_SECTION.convert(key, value)
        val rewardsSection = section.get("rewards", ConfigAdapter.CONFIG_SECTION)

        val criteria = section.get("criteria", ConfigAdapter.LIST.from(CriteriaConfigAdapter)) ?: listOf()
        val advancement = RebarAdvancement(
            key?.let { NamespacedKey.fromString(key) }
                ?: throw ExceptionInInitializerError("RebarAdvancement must have a key"),
            section.get("parent", ConfigAdapter.NAMESPACED_KEY),
            section.get("display-info", DisplayInfoConfigAdapter),
            section.get("rewards", ConfigAdapter.CONFIG_SECTION)?.let {
                RebarAdvancementRewards(
                    rewardsSection?.get("experience", ConfigAdapter.INTEGER) ?: 0,
                    rewardsSection?.get("recipes", ConfigAdapter.LIST.from(ConfigAdapter.NAMESPACED_KEY)) ?: listOf(),
                    rewardsSection?.get("loot", ConfigAdapter.LIST.from(ConfigAdapter.NAMESPACED_KEY)) ?: listOf(),
                    rewardsSection?.get("function", ConfigAdapter.NAMESPACED_KEY)
                )
            } ?: RebarAdvancementRewards(
                0,
                listOf(),
                listOf(),
                null
            ),
            criteria,
            section.get("requirements", ConfigAdapter.LIST.from(ConfigAdapter.LIST.from(ConfigAdapter.NAMESPACED_KEY)))
                ?: listOf(criteria.map { it.key })
        )
        advancement.register()
        return advancement
    }

    object CriteriaConfigAdapter : ConfigAdapter<Criterion> {
        override val type: Type
            get() = Criteria::class.java

        override fun convert(key: String?, value: Any): Criterion {
            val section = ConfigAdapter.CONFIG_SECTION.convert(key, value)
            val criterionKey = section.getOrThrow("key", ConfigAdapter.NAMESPACED_KEY)
            val criteriaTypeKey = section.getOrThrow("type", ConfigAdapter.NAMESPACED_KEY)
            val criteria = RebarRegistry.CRITERIA[criteriaTypeKey] ?: throw ExceptionInInitializerError("criteria type $criteriaTypeKey was not defined in registry, did you remember to call register()?")
            return criteria.createCriterion(criterionKey, section)
        }

    }

    object DisplayInfoConfigAdapter : ConfigAdapter<RebarAdvancementDisplayInfo> {
        override val type: Type
            get() = RebarAdvancementDisplayInfo::class.java

        override fun convert(key: String?, value: Any): RebarAdvancementDisplayInfo {
            val section = ConfigAdapter.CONFIG_SECTION.convert(key, value)
            val iconSection = section.getOrThrow("icon", ConfigAdapter.CONFIG_SECTION)
            return RebarAdvancementDisplayInfo(
                RebarAdvancementIcon(
                    iconSection.getOrThrow("item", ConfigAdapter.NAMESPACED_KEY),
                    iconSection.get("count", ConfigAdapter.INTEGER) ?: 1
                ),
                section.get("title", ConfigAdapter.STRING)?.let { Component.translatable(it) },
                section.get("description", ConfigAdapter.STRING)?.let { Component.translatable(it) },
                section.get("frame", ConfigAdapter.STRING) ?: "task",
                section.get("background", ConfigAdapter.NAMESPACED_KEY),
                section.get("show-toast", ConfigAdapter.BOOLEAN) ?: true,
                section.get("announce-to-chat", ConfigAdapter.BOOLEAN) ?: true,
                section.get("hidden", ConfigAdapter.BOOLEAN) ?: false
            )
        }
    }
}