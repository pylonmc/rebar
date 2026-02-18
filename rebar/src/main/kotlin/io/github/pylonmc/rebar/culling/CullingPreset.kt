package io.github.pylonmc.rebar.culling

import io.github.pylonmc.rebar.config.RebarConfig
import org.bukkit.Material

@JvmRecord
data class CullingPreset(
    val index: Int,
    val id: String,
    val material: Material,

    val updateInterval: Int,
    val hiddenInterval: Int,
    val visibleInterval: Int,

    val alwaysShowRadius: Int,
    val cullRadius: Int,

    val maxOccludingCount: Int,
) {
    fun matches(config: PlayerCullingConfig): Boolean {
        return updateInterval == config.updateInterval
                && hiddenInterval == config.hiddenInterval
                && visibleInterval == config.visibleInterval
                && alwaysShowRadius == config.alwaysShowRadius
                && cullRadius == config.cullRadius
                && maxOccludingCount == config.maxOccludingCount
    }

    fun toPlayerConfig() : PlayerCullingConfig {
        return PlayerCullingConfig(
            updateInterval = updateInterval,
            hiddenInterval = hiddenInterval,
            visibleInterval = visibleInterval,
            alwaysShowRadius = alwaysShowRadius,
            cullRadius = cullRadius,
            maxOccludingCount = maxOccludingCount
        )
    }

    fun assertValid() {
        checkValid("update-interval", updateInterval, RebarConfig.CullingEngineConfig.UPDATE_INTERVAL_LIMIT)
        checkValid("hidden-interval", hiddenInterval, RebarConfig.CullingEngineConfig.HIDDEN_INTERVAL_LIMIT)
        checkValid("visible-interval", visibleInterval, RebarConfig.CullingEngineConfig.VISIBLE_INTERVAL_LIMIT)
        checkValid("always-show-radius", alwaysShowRadius, RebarConfig.CullingEngineConfig.ALWAYS_SHOW_RADIUS_LIMIT)
        checkValid("cull-radius", cullRadius, RebarConfig.CullingEngineConfig.CULL_RADIUS_LIMIT)
        checkValid("max-occluding-count", maxOccludingCount, RebarConfig.CullingEngineConfig.MAX_OCCLUDING_COUNT_LIMIT)
    }

    private fun checkValid(propertyName: String, value: Int, range: IntRange) {
        check(value in range) { "$propertyName must be between ${range.first} and ${range.last}, but was $value" }
    }
}

