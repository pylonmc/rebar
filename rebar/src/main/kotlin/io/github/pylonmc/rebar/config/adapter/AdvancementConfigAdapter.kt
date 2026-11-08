package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.advancements.*
import io.github.pylonmc.rebar.advancements.reward.RebarAdvancementReward
import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.item.ItemTypeWrapper
import io.github.pylonmc.rebar.registry.RebarRegistry
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import java.lang.reflect.Type
import java.util.Dictionary

object AdvancementConfigAdapter : ConfigAdapter<RebarAdvancement> {
    private val DISPLAY_INFO_ADAPTER = AdvancementDisplayInfoConfigAdapter
    private val REWARD_ADAPTER = AdvancementRewardConfigAdapter
    private val CRITERIA_ADAPTER = AdvancementCriteriaConfigAdapter

    override val type: Type
        get() = RebarAdvancement::class.java

    override fun convert(value: Any): RebarAdvancement {
        val section = ConfigAdapter.CONFIG_SECTION.convert(value)
        val advancementKey = section.name?.let { NamespacedKey.fromString(it) } ?: throw ExceptionInInitializerError("RebarAdvancement must have a key")
        return RebarAdvancement(
            advancementKey,
            section.get("parent", ConfigAdapter.NAMESPACED_KEY),
            section.get("display-info", DISPLAY_INFO_ADAPTER),
            section.get("rewards", ConfigAdapter.LIST.from(REWARD_ADAPTER)) ?: listOf(),
            section.get("criteria", ConfigAdapter.LIST.from(CRITERIA_ADAPTER)) ?: listOf(),
            section.get("requirements", ConfigAdapter.LIST.from(ConfigAdapter.LIST.from(ConfigAdapter.NAMESPACED_KEY))) ?: listOf()
        )
    }
}