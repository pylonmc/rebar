package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.culling.PlayerCullingConfig
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

object PlayerCullingConfigPersistentDataType : PersistentDataType<PersistentDataContainer, PlayerCullingConfig> {
    val updateIntervalKey = rebarKey("update_interval")
    val hiddenIntervalKey = rebarKey("hidden_interval")
    val visibleIntervalKey = rebarKey("visible_interval")

    val alwaysShowRadiusKey = rebarKey("always_show_radius")
    val cullRadiusKey = rebarKey("cull_radius")

    val maxOccludingCountKey = rebarKey("max_occluding_count")

    override fun getPrimitiveType(): Class<PersistentDataContainer> = PersistentDataContainer::class.java

    override fun getComplexType(): Class<PlayerCullingConfig> = PlayerCullingConfig::class.java

    override fun fromPrimitive(
        primitive: PersistentDataContainer,
        context: PersistentDataAdapterContext
    ): PlayerCullingConfig {
        val defaultSettings = RebarConfig.CullingEngineConfig.DEFAULT_CULLING_PRESET
        val updateInterval = primitive.getOrDefault(updateIntervalKey, RebarSerializers.INTEGER, defaultSettings.updateInterval)
        val hiddenInterval = primitive.getOrDefault(hiddenIntervalKey, RebarSerializers.INTEGER, defaultSettings.hiddenInterval)
        val visibleInterval = primitive.getOrDefault(visibleIntervalKey, RebarSerializers.INTEGER, defaultSettings.visibleInterval)
        val alwaysShowRadius = primitive.getOrDefault(alwaysShowRadiusKey, RebarSerializers.INTEGER, defaultSettings.alwaysShowRadius)
        val cullRadius = primitive.getOrDefault(cullRadiusKey, RebarSerializers.INTEGER, defaultSettings.cullRadius)
        val maxOccludingCount = primitive.getOrDefault(maxOccludingCountKey, RebarSerializers.INTEGER, defaultSettings.maxOccludingCount)
        return PlayerCullingConfig(
            updateInterval,
            hiddenInterval,
            visibleInterval,
            alwaysShowRadius,
            cullRadius,
            maxOccludingCount
        )
    }

    override fun toPrimitive(complex: PlayerCullingConfig, context: PersistentDataAdapterContext): PersistentDataContainer {
        val pdc = context.newPersistentDataContainer()
        pdc.set(updateIntervalKey, RebarSerializers.INTEGER, complex.updateInterval)
        pdc.set(hiddenIntervalKey, RebarSerializers.INTEGER, complex.hiddenInterval)
        pdc.set(visibleIntervalKey, RebarSerializers.INTEGER, complex.visibleInterval)
        pdc.set(alwaysShowRadiusKey, RebarSerializers.INTEGER, complex.alwaysShowRadius)
        pdc.set(cullRadiusKey, RebarSerializers.INTEGER, complex.cullRadius)
        pdc.set(maxOccludingCountKey, RebarSerializers.INTEGER, complex.maxOccludingCount)
        return pdc
    }
}