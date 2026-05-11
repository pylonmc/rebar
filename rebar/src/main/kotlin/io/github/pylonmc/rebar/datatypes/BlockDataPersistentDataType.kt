package io.github.pylonmc.rebar.datatypes

import org.bukkit.Bukkit
import org.bukkit.block.data.BlockData
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType

object BlockDataPersistentDataType : PersistentDataType<String, BlockData> {

    override fun getPrimitiveType(): Class<String> = String::class.java

    override fun getComplexType(): Class<BlockData> = BlockData::class.java

    override fun fromPrimitive(
        primitive: String,
        context: PersistentDataAdapterContext
    ): BlockData {
        return Bukkit.createBlockData(primitive)
    }

    override fun toPrimitive(complex: BlockData, context: PersistentDataAdapterContext): String {
        return complex.asString
    }
}