package io.github.pylonmc.rebar.config.adapter

import com.destroystokyo.paper.MaterialTags
import io.github.pylonmc.rebar.block.BlockTypeWrapper
import io.github.pylonmc.rebar.block.BlockTypeWrapper.Companion.toBlockTypeTag
import io.github.pylonmc.rebar.registry.RebarRegistry
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import java.lang.reflect.Modifier

object BlockTagConfigAdapter : ConfigAdapter<Tag<BlockTypeWrapper>> {
    override val type = Tag::class.java

    override fun convert(value: Any): Tag<BlockTypeWrapper> {
        val string = ConfigAdapter.STRING.convert(value)
        if (!string.startsWith("#")) {
            throw IllegalArgumentException("Block tag must start with '#': $value")
        }

        val tagKey = NamespacedKey.fromString(string.drop(1)) ?: throw IllegalArgumentException("Invalid tag: $value")

        // Allow all block tags
        val blockTag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, tagKey, Material::class.java)
        if (blockTag != null) {
            return blockTag.toBlockTypeTag()
        }

        // Allow item tags, but only if they can be translated to items successfully
        val itemTag = Bukkit.getTag(Tag.REGISTRY_ITEMS, tagKey, Material::class.java)
        if (itemTag != null) {
            if (itemTag.values.any { !it.isBlock }) {
                throw IllegalArgumentException("Item tag detected, but invalid due to it containing an item that can't be translated to block")
            }

            return itemTag.toBlockTypeTag()
        }

        // Allow usage of paper's material tag registry, which is separate from the bukkit one
        val paperTag = paperRegistry[tagKey]
        if (paperTag != null) {
            return paperTag
        }

        // Check our own tags
        val rebarTag = RebarRegistry.BLOCK_TAGS[tagKey]
        if (rebarTag != null) {
            return rebarTag
        }

        throw IllegalArgumentException("Block tag not found: $value")
    }

    private val paperRegistry = buildMap {
        for (entry in MaterialTags::class.java.declaredFields) {
            if (entry.modifiers and Modifier.STATIC == 0) continue

            val value = entry.get(null) ?: continue
            if (value !is Tag<*>) continue

            val content = value.values.first()
            if (content !is Material) continue

            @Suppress("UNCHECKED_CAST")
            val realTag = value as Tag<Material>

            if (realTag.values.any { !it.isBlock }) continue

            this[realTag.key] = realTag.toBlockTypeTag()
        }
    }
}