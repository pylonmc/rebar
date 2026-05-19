package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.advancements.RebarAdvancement
import io.github.pylonmc.rebar.advancements.RebarAdvancementCriterionInfo
import io.github.pylonmc.rebar.advancements.RebarAdvancementDisplayInfo
import io.github.pylonmc.rebar.advancements.RebarAdvancementIcon
import io.github.pylonmc.rebar.advancements.RebarAdvancementRewards
import net.kyori.adventure.text.Component
import java.lang.reflect.Type

object AdvancementConfigAdapter : ConfigAdapter<RebarAdvancement> {
    override val type: Type
        get() = RebarAdvancement::class.java

    override fun convert(value: Any): RebarAdvancement {
        val section = ConfigAdapter.CONFIG_SECTION.convert(value)
        val rewardsSection = section.getOrThrow("rewards", ConfigAdapter.CONFIG_SECTION) // TODO: make this skippable

        return RebarAdvancement(
            section.get("parent", ConfigAdapter.NAMESPACED_KEY),
            section.get("displayInfo", DisplayInfoConfigAdapter),
            RebarAdvancementRewards(
                rewardsSection.get("experience", ConfigAdapter.INTEGER) ?: 0,
                rewardsSection.get("recipes", ConfigAdapter.LIST.from(ConfigAdapter.NAMESPACED_KEY)) ?: listOf(),
                rewardsSection.get("loot", ConfigAdapter.LIST.from(ConfigAdapter.NAMESPACED_KEY)) ?: listOf(),
                rewardsSection.get("function", ConfigAdapter.NAMESPACED_KEY)
            ),
            section.get("criteria", ConfigAdapter.LIST.from(ConfigAdapter.NAMESPACED_KEY)) ?: listOf(),
            section.get("requirements", ConfigAdapter.LIST.from(ConfigAdapter.LIST.from(ConfigAdapter.STRING))) ?: listOf(),
            Component.translatable(section.getOrThrow("name", ConfigAdapter.NAMESPACED_KEY).key)
        )
    }

    object DisplayInfoConfigAdapter : ConfigAdapter<RebarAdvancementDisplayInfo> {
        override val type: Type
            get() = RebarAdvancementDisplayInfo::class.java

        override fun convert(value: Any): RebarAdvancementDisplayInfo {
            val section = ConfigAdapter.CONFIG_SECTION.convert(value)
            val iconSection = section.getOrThrow("icon", ConfigAdapter.CONFIG_SECTION)
            return RebarAdvancementDisplayInfo(
                RebarAdvancementIcon(
                    iconSection.getOrThrow("item", ConfigAdapter.NAMESPACED_KEY),
                    iconSection.get("count", ConfigAdapter.INTEGER) ?: 1
                ),
                Component.translatable(section.getOrThrow("title", ConfigAdapter.NAMESPACED_KEY).key),
                Component.translatable(section.getOrThrow("description", ConfigAdapter.NAMESPACED_KEY).key),
                section.get("frame", ConfigAdapter.STRING) ?: "task",
                section.get("background", ConfigAdapter.NAMESPACED_KEY),
                section.get("showToast", ConfigAdapter.BOOLEAN) ?: true,
                section.get("announceToChat", ConfigAdapter.BOOLEAN) ?: true,
                section.get("hidden", ConfigAdapter.BOOLEAN) ?: false
            )
        }
    }
}