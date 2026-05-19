package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.advancements.RebarAdvancement
import io.github.pylonmc.rebar.advancements.RebarAdvancementDisplayInfo
import io.github.pylonmc.rebar.advancements.RebarAdvancementIcon
import io.github.pylonmc.rebar.advancements.RebarAdvancementRewards
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import java.lang.reflect.Type

object AdvancementConfigAdapter : ConfigAdapter<RebarAdvancement> {
    override val type: Type
        get() = RebarAdvancement::class.java

    override fun convert(key: String?, value: Any): RebarAdvancement {
        val section = ConfigAdapter.CONFIG_SECTION.convert(key, value)
        val rewardsSection = section.getOrThrow("rewards", ConfigAdapter.CONFIG_SECTION) // TODO: make this skippable

        return RebarAdvancement(
            key?.let { NamespacedKey.fromString(key) } ?: throw ExceptionInInitializerError("RebarAdvancement must have a key"),
            section.get("parent", ConfigAdapter.NAMESPACED_KEY),
            section.get("display-info", DisplayInfoConfigAdapter),
            section.get("rewards", ConfigAdapter.CONFIG_SECTION)?.let {
                RebarAdvancementRewards(
                    rewardsSection.get("experience", ConfigAdapter.INTEGER) ?: 0,
                    rewardsSection.get("recipes", ConfigAdapter.LIST.from(ConfigAdapter.NAMESPACED_KEY)) ?: listOf(),
                    rewardsSection.get("loot", ConfigAdapter.LIST.from(ConfigAdapter.NAMESPACED_KEY)) ?: listOf(),
                    rewardsSection.get("function", ConfigAdapter.NAMESPACED_KEY)
                )
            } ?: RebarAdvancementRewards(
                0,
                listOf(),
                listOf(),
                null
            ),
            section.get("criteria", ConfigAdapter.LIST.from(ConfigAdapter.NAMESPACED_KEY)) ?: listOf(),
            section.get("requirements", ConfigAdapter.LIST.from(ConfigAdapter.LIST.from(ConfigAdapter.STRING)))
                ?: listOf()
        )
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