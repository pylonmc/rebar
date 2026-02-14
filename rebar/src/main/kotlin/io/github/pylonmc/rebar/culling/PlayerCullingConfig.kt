package io.github.pylonmc.rebar.culling

import io.github.pylonmc.rebar.config.RebarConfig

class PlayerCullingConfig(
    var updateInterval: Int,
    var hiddenInterval: Int,
    var visibleInterval: Int,

    var alwaysShowRadius: Int,
    var cullRadius: Int,

    var maxOccludingCount: Int
) {
    fun copy(): PlayerCullingConfig {
        return PlayerCullingConfig(
            updateInterval = updateInterval,
            hiddenInterval = hiddenInterval,
            visibleInterval = visibleInterval,
            alwaysShowRadius = alwaysShowRadius,
            cullRadius = cullRadius,
            maxOccludingCount = maxOccludingCount
        )
    }

    fun repairInvalid() = apply {
        updateInterval = updateInterval.coerceIn(RebarConfig.CullingEngineConfig.UPDATE_INTERVAL_LIMIT)
        hiddenInterval = hiddenInterval.coerceIn(RebarConfig.CullingEngineConfig.HIDDEN_INTERVAL_LIMIT)
        visibleInterval = visibleInterval.coerceIn(RebarConfig.CullingEngineConfig.VISIBLE_INTERVAL_LIMIT)
        alwaysShowRadius = alwaysShowRadius.coerceIn(RebarConfig.CullingEngineConfig.ALWAYS_SHOW_RADIUS_LIMIT)
        cullRadius = cullRadius.coerceIn(RebarConfig.CullingEngineConfig.CULL_RADIUS_LIMIT)
        maxOccludingCount = maxOccludingCount.coerceIn(RebarConfig.CullingEngineConfig.MAX_OCCLUDING_COUNT_LIMIT)
    }
}
