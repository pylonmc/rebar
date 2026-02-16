package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.resourcepack.block.CullingPreset
import java.lang.reflect.Type

object CullingPresetConfigAdapter : ConfigAdapter<CullingPreset> {
    override val type: Type = CullingPreset::class.java

    override fun convert(value: Any): CullingPreset {
        val section = ConfigAdapter.CONFIG_SECTION.convert(value)
        return CullingPreset(
            index = section.getOrThrow("index", ConfigAdapter.INT),
            id = section.getOrThrow("id", ConfigAdapter.STRING),
            material = section.getOrThrow("material", ConfigAdapter.MATERIAL),
            updateInterval = section.getOrThrow("update-interval", ConfigAdapter.INT),
            hiddenInterval = section.get("hidden-interval", ConfigAdapter.INT, 1),
            visibleInterval = section.get("visible-interval", ConfigAdapter.INT, 20),
            alwaysShowRadius = section.get("always-show-radius", ConfigAdapter.INT, 16),
            cullRadius = section.get("cull-radius", ConfigAdapter.INT, 64),
            maxOccludingCount = section.get("max-occluding-count", ConfigAdapter.INT, 3)
        )
    }
}