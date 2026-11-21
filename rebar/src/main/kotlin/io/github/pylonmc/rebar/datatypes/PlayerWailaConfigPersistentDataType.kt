package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.waila.PlayerWailaConfig
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

object PlayerWailaConfigPersistentDataType : PersistentDataType<PersistentDataContainer, PlayerWailaConfig> {
    val enabledKey = rebarKey("waila_enabled", RebarSerializers.BOOLEAN)
    val vanillaWailaEnabledKey = rebarKey("waila_vanilla_enabled", RebarSerializers.BOOLEAN)
    val typeKey = rebarKey("waila_type", RebarSerializers.WAILA_TYPE)

    override fun getPrimitiveType(): Class<PersistentDataContainer> = PersistentDataContainer::class.java

    override fun getComplexType(): Class<PlayerWailaConfig> = PlayerWailaConfig::class.java

    override fun fromPrimitive(
        primitive: PersistentDataContainer,
        context: PersistentDataAdapterContext
    ): PlayerWailaConfig {
        val enabled = primitive.get(enabledKey)!!
        val vanillaWailaEnabled = primitive.get(vanillaWailaEnabledKey)!!
        val type = primitive.get(typeKey)!!
        return PlayerWailaConfig(enabled, vanillaWailaEnabled, type)
    }

    override fun toPrimitive(
        complex: PlayerWailaConfig,
        context: PersistentDataAdapterContext
    ): PersistentDataContainer {
        val pdc = context.newPersistentDataContainer()
        pdc.set(enabledKey, complex.enabled)
        pdc.set(vanillaWailaEnabledKey, complex.vanillaWailaEnabled)
        pdc.set(typeKey, complex.type)
        return pdc
    }
}