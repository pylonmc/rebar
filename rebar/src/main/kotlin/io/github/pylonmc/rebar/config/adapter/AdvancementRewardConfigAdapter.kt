package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.advancements.RebarAdvancementRewardSpec
import org.bukkit.NamespacedKey
import java.lang.reflect.Type

object AdvancementRewardConfigAdapter : ConfigAdapter<RebarAdvancementRewardSpec> {
    override val type: Type
        get() = RebarAdvancementRewardSpec::class.java

    override fun convert(value: Any): RebarAdvancementRewardSpec {
        val section = ConfigAdapter.CONFIG_SECTION.convert(value)
        val rewardTypeKey = section.name?.let { NamespacedKey.fromString(it) } ?: throw ExceptionInInitializerError("RebarAdvancementRewardSpec must have a key")
        return RebarAdvancementRewardSpec(rewardTypeKey, section)
    }

}