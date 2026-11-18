package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.util.gui.ProgressItem
import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.util.setNullable
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

object ProgressItemPersistentDataType : PersistentDataType<PersistentDataContainer, ProgressItem> {
    val itemStackKey = rebarKey("item_stack", RebarSerializers.ITEM_STACK)
    val countDownKey = rebarKey("count_down", RebarSerializers.BOOLEAN)
    val totalTimeKey = rebarKey("total_time", RebarSerializers.DURATION)
    val progressKey = rebarKey("progress", RebarSerializers.DOUBLE)

    override fun getPrimitiveType(): Class<PersistentDataContainer> = PersistentDataContainer::class.java

    override fun getComplexType(): Class<ProgressItem> = ProgressItem::class.java

    override fun fromPrimitive(
        primitive: PersistentDataContainer,
        context: PersistentDataAdapterContext
    ): ProgressItem {
        val stack = primitive.get(itemStackKey)!!
        val countDown = primitive.get(countDownKey)!!
        val item = ProgressItem(stack, countDown)
        item.totalTime = primitive.get(totalTimeKey)
        item.progress = primitive.get(progressKey)!!
        return item
    }

    override fun toPrimitive(complex: ProgressItem, context: PersistentDataAdapterContext): PersistentDataContainer {
        val pdc = context.newPersistentDataContainer()
        pdc.set(itemStackKey, complex.lastDisplayedItem)
        pdc.set(countDownKey, complex.countDown)
        pdc.setNullable(totalTimeKey, complex.totalTime)
        pdc.set(progressKey, complex.progress)
        return pdc
    }
}