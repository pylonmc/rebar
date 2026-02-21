package io.github.pylonmc.rebar.datatypes

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType


object ComponentPersistentDataType : PersistentDataType<String, Component> {

    private val serializer = GsonComponentSerializer.gson()

    override fun getPrimitiveType(): Class<String> = String::class.java

    override fun getComplexType(): Class<Component> = Component::class.java

    override fun fromPrimitive(
        primitive: String,
        context: PersistentDataAdapterContext
    ): Component {
        return serializer.deserialize(primitive)
    }

    override fun toPrimitive(complex: Component, context: PersistentDataAdapterContext): String {
        return serializer.serialize(complex)
    }

}