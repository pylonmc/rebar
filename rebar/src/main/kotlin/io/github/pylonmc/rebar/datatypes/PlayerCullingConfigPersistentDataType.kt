package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.culling.PlayerCullingConfig
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

object PlayerCullingConfigPersistentDataType : PersistentDataType<PersistentDataContainer, PlayerCullingConfig> {
    val updateIntervalKey = rebarKey("update_interval", RebarSerializers.INTEGER)
    val hiddenIntervalKey = rebarKey("hidden_interval", RebarSerializers.INTEGER)
    val visibleIntervalKey = rebarKey("visible_interval", RebarSerializers.INTEGER)

    val alwaysShowRadiusKey = rebarKey("always_show_radius", RebarSerializers.INTEGER)
    val cullRadiusKey = rebarKey("cull_radius", RebarSerializers.INTEGER)

    val maxOccludingCountKey = rebarKey("max_occluding_count", RebarSerializers.INTEGER)

    override fun getPrimitiveType(): Class<PersistentDataContainer> = PersistentDataContainer::class.java

    override fun getComplexType(): Class<PlayerCullingConfig> = PlayerCullingConfig::class.java

    override fun fromPrimitive(
        primitive: PersistentDataContainer,
        context: PersistentDataAdapterContext
    ): PlayerCullingConfig {
        val defaultSettings = RebarConfig.CullingEngineConfig.DEFAULT_CULLING_PRESET
        val updateInterval = primitive.getOrDefault(updateIntervalKey, defaultSettings.updateInterval)
        val hiddenInterval = primitive.getOrDefault(hiddenIntervalKey, defaultSettings.hiddenInterval)
        val visibleInterval = primitive.getOrDefault(visibleIntervalKey, defaultSettings.visibleInterval)
        val alwaysShowRadius = primitive.getOrDefault(alwaysShowRadiusKey, defaultSettings.alwaysShowRadius)
        val cullRadius = primitive.getOrDefault(cullRadiusKey, defaultSettings.cullRadius)
        val maxOccludingCount = primitive.getOrDefault(maxOccludingCountKey, defaultSettings.maxOccludingCount)
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
        pdc.set(updateIntervalKey, complex.updateInterval)
        pdc.set(hiddenIntervalKey, complex.hiddenInterval)
        pdc.set(visibleIntervalKey, complex.visibleInterval)
        pdc.set(alwaysShowRadiusKey, complex.alwaysShowRadius)
        pdc.set(cullRadiusKey, complex.cullRadius)
        pdc.set(maxOccludingCountKey, complex.maxOccludingCount)
        return pdc
    }
}