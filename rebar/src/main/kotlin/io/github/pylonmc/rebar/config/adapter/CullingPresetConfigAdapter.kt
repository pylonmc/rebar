package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.culling.CullingPreset
import java.lang.reflect.Type

object CullingPresetConfigAdapter : ConfigAdapter<CullingPreset> {
    override val type: Type = CullingPreset::class.java

    override fun convert(value: Any): CullingPreset {
        val section = ConfigAdapter.CONFIG_SECTION.convert(value)
        return CullingPreset(
            index = section.getOrThrow("index", ConfigAdapter.INTEGER),
            id = section.getOrThrow("id", ConfigAdapter.STRING),
            material = section.getOrThrow("material", ConfigAdapter.MATERIAL),
            updateInterval = section.getOrThrow("update-interval", ConfigAdapter.INTEGER),
            hiddenInterval = section.get("hidden-interval", ConfigAdapter.INTEGER, 1),
            visibleInterval = section.get("visible-interval", ConfigAdapter.INTEGER, 20),
            alwaysShowRadius = section.get("always-show-radius", ConfigAdapter.INTEGER, 16),
            cullRadius = section.get("cull-radius", ConfigAdapter.INTEGER, 64),
            maxOccludingCount = section.get("max-occluding-count", ConfigAdapter.INTEGER, 3)
        ).also { it.assertValid() }
    }
}