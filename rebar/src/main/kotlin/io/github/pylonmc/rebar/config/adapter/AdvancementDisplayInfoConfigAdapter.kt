package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.advancements.RebarAdvancementDisplayInfo
import io.github.pylonmc.rebar.advancements.RebarAdvancementIcon
import io.github.pylonmc.rebar.item.ItemTypeWrapper
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import java.lang.reflect.Type

object AdvancementDisplayInfoConfigAdapter : ConfigAdapter<RebarAdvancementDisplayInfo> {
    override val type: Type
        get() = RebarAdvancementDisplayInfo::class.java

    override fun convert(value: Any): RebarAdvancementDisplayInfo {
        val section = ConfigAdapter.CONFIG_SECTION.convert(value)
        val iconSection = section.getOrThrow("icon", ConfigAdapter.CONFIG_SECTION)
        val advancementKey = NamespacedKey.fromString(section.internalSection.parent!!.name)!!
        return RebarAdvancementDisplayInfo(
            RebarAdvancementIcon(
                ItemTypeWrapper(iconSection.getOrThrow("item", ConfigAdapter.NAMESPACED_KEY)),
                iconSection.get("count", ConfigAdapter.INTEGER) ?: 1
            ),
            Component.translatable("${advancementKey.namespace}.advancements.${advancementKey.key}.title"),
            Component.translatable("${advancementKey.namespace}.advancements.${advancementKey.key}.description"),
            section.get("frame", ConfigAdapter.STRING) ?: "task",
            section.get("background", ConfigAdapter.NAMESPACED_KEY),
            section.get("show-toast", ConfigAdapter.BOOLEAN) ?: true,
            section.get("announce-to-chat", ConfigAdapter.BOOLEAN) ?: true,
            section.get("hidden", ConfigAdapter.BOOLEAN) ?: false
        )
    }
}