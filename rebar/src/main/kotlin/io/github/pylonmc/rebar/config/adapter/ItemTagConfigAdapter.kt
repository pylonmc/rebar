package io.github.pylonmc.rebar.config.adapter

import com.destroystokyo.paper.MaterialTags
import io.github.pylonmc.rebar.item.ItemTypeWrapper
import io.github.pylonmc.rebar.item.ItemTypeWrapper.Companion.toItemTypeTag
import io.github.pylonmc.rebar.registry.RebarRegistry
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import java.lang.reflect.Modifier

object ItemTagConfigAdapter : ConfigAdapter<Tag<ItemTypeWrapper>> {
    override val type = Tag::class.java

    override fun convert(key: String?, value: Any): Tag<ItemTypeWrapper> {
        val string = ConfigAdapter.STRING.convert(key, value)
        if (!string.startsWith("#")) {
            throw IllegalArgumentException("Item tag must start with '#': $value")
        }

        val tagKey = NamespacedKey.fromString(string.drop(1)) ?: throw IllegalArgumentException("Invalid tag: $value")

        // Allow all item tags
        val itemTag = Bukkit.getTag(Tag.REGISTRY_ITEMS, tagKey, Material::class.java)
        if (itemTag != null) {
            return itemTag.toItemTypeTag()
        }

        // Allow block tags, but only if they can be translated to items successfully
        val blockTag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, tagKey, Material::class.java)
        if (blockTag != null) {
            if (blockTag.values.any { !it.isItem }) {
                throw IllegalArgumentException("Block tag detected, but invalid due to it containing a block that can't be translated to item")
            }

            return blockTag.toItemTypeTag()
        }

        // Allow usage of paper's material tag registry, which is separate from the bukkit one
        val paperTag = paperRegistry[tagKey]
        if (paperTag != null) {
            return paperTag
        }

        // Check our own tags
        val rebarTag = RebarRegistry.ITEM_TAGS[tagKey]
        if (rebarTag != null) {
            return rebarTag
        }

        throw IllegalArgumentException("Item tag not found: $value")
    }

    private val paperRegistry = object : HashMap<NamespacedKey, Tag<ItemTypeWrapper>>() {
        init {
            for (entry in MaterialTags::class.java.declaredFields) {
                if (entry.modifiers and Modifier.STATIC == 0) continue

                val value = entry.get(null) ?: continue
                if (value !is Tag<*>) continue

                val content = value.values.first()
                if (content !is Material) continue

                @Suppress("UNCHECKED_CAST")
                val realTag = value as Tag<Material>

                this[realTag.key] = realTag.toItemTypeTag()
            }
        }

        override operator fun get(key: NamespacedKey): Tag<ItemTypeWrapper>? {
            if (key.namespace != "paper") return null
            return super[key]
        }
    }
}