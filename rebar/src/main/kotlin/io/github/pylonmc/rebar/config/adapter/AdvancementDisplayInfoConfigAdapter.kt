package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.advancements.RebarAdvancementDisplayInfo
import io.github.pylonmc.rebar.advancements.RebarAdvancementIcon
import io.papermc.paper.advancement.AdvancementDisplay
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import java.lang.reflect.Type

/**
 * Parses the `display-info` of the advancement [advancementKey]. The title and description are always the
 * translation keys `<namespace>.advancements.<key>.title` and `<namespace>.advancements.<key>.description`.
 */
class AdvancementDisplayInfoConfigAdapter(private val advancementKey: NamespacedKey) : ConfigAdapter<RebarAdvancementDisplayInfo> {

    override val type: Type = RebarAdvancementDisplayInfo::class.java

    override fun convert(value: Any): RebarAdvancementDisplayInfo {
        val section = ConfigAdapter.CONFIG_SECTION.convert(value)
        val iconSection = section.getSectionOrThrow("icon")
        val iconCount = iconSection.getIfPresent("count", ConfigAdapter.INTEGER) ?: 1
        require(iconCount in 1..99) { "Advancement icon count must be between 1 and 99, but was $iconCount" }
        return RebarAdvancementDisplayInfo(
            RebarAdvancementIcon(
                iconSection.getOrThrow("item", ConfigAdapter.ITEM_TYPE_WRAPPER),
                iconCount
            ),
            Component.translatable("${advancementKey.namespace}.advancements.${advancementKey.key}.title"),
            Component.translatable("${advancementKey.namespace}.advancements.${advancementKey.key}.description"),
            section.getIfPresent("frame", FRAME) ?: AdvancementDisplay.Frame.TASK,
            section.getIfPresent("background", ConfigAdapter.NAMESPACED_KEY),
            section.getIfPresent("show-toast", ConfigAdapter.BOOLEAN) ?: true,
            section.getIfPresent("announce-to-chat", ConfigAdapter.BOOLEAN) ?: true,
            section.getIfPresent("hidden", ConfigAdapter.BOOLEAN) ?: false
        )
    }

    private companion object {
        val FRAME = ConfigAdapter.ENUM.from<AdvancementDisplay.Frame>()
    }
}
