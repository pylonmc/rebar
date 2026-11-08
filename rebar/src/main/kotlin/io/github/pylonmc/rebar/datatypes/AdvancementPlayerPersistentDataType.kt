package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.advancements.AdvancementPlayerPersistentData
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

object AdvancementPlayerPersistentDataType : PersistentDataType<PersistentDataContainer, AdvancementPlayerPersistentData> {
    val ADVANCEMENT_KEY_KEY = rebarKey("advancement_key")
    val COMPLETED_CRITERIA_KEY = rebarKey("completed_criteria")
    val COMPLETE_KEY = rebarKey("complete")

    override fun getPrimitiveType(): Class<PersistentDataContainer>
        = PersistentDataContainer::class.java

    override fun getComplexType(): Class<AdvancementPlayerPersistentData>
        = AdvancementPlayerPersistentData::class.java

    override fun toPrimitive(
        complex: AdvancementPlayerPersistentData,
        context: PersistentDataAdapterContext
    ): PersistentDataContainer {
        val pdc = context.newPersistentDataContainer()
        pdc.set(ADVANCEMENT_KEY_KEY, RebarSerializers.NAMESPACED_KEY, complex.key)
        pdc.set(COMPLETED_CRITERIA_KEY, RebarSerializers.SET.setTypeFrom(PersistentDataType.STRING), complex.completedCriteria)
        pdc.set(COMPLETE_KEY, PersistentDataType.BOOLEAN, complex.completed)
        return pdc
    }

    override fun fromPrimitive(
        primitive: PersistentDataContainer,
        context: PersistentDataAdapterContext
    ): AdvancementPlayerPersistentData {
        val advancementKey = primitive.get(ADVANCEMENT_KEY_KEY, RebarSerializers.NAMESPACED_KEY)!!
        val completedCriteria = primitive.get(COMPLETED_CRITERIA_KEY, RebarSerializers.SET.setTypeFrom(PersistentDataType.STRING))!!
        val complete = primitive.get(COMPLETE_KEY, PersistentDataType.BOOLEAN)!!
        return AdvancementPlayerPersistentData(advancementKey, completedCriteria, complete)
    }
}