package io.github.pylonmc.rebar.block

import io.github.pylonmc.rebar.block.context.BlockCreateContext
import io.github.pylonmc.rebar.util.findConstructorMatching
import io.github.pylonmc.rebar.util.getAddon
import io.github.pylonmc.rebar.util.position.BlockPosition
import io.github.pylonmc.rebar.util.position.position
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import org.bukkit.Keyed
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.persistence.PersistentDataContainer
import java.lang.invoke.MethodHandle

/**
 * Stores information about a Rebar block type, including its key, material, and class.
 *
 * You should generally not need to use this directly in addons.
 */
class RebarBlockSchema(
    private val key: NamespacedKey,
    val material: Material,
    val blockClass: Class<out RebarBlock>,
) : Keyed {

    init {
        check(material.isBlock) { "Material $material is not a block" }
    }

    val addon = getAddon(key)

    val nameTranslationKey: TranslatableComponent
    val loreTranslationKey: TranslatableComponent

    init {
        val prefix = "${key.namespace}.item.${key.key}"
        nameTranslationKey = Component.translatable("$prefix.name")
        loreTranslationKey = Component.translatable("$prefix.lore")
        val default = "$prefix.waila"
    }

    private val createConstructor: MethodHandle = blockClass.findConstructorMatching(
        Block::class.java,
        BlockCreateContext::class.java
    ) ?: throw NoSuchMethodException(
        "Block '$key' ($blockClass) is missing a create constructor (${javaClass.simpleName}, Block, BlockCreateContext)"
    )

    private val loadConstructor: MethodHandle = blockClass.findConstructorMatching(
        Block::class.java,
        PersistentDataContainer::class.java
    ) ?: throw NoSuchMethodException(
        "Block '$key' ($blockClass) is missing a load constructor (${javaClass.simpleName}, Block, PersistentDataContainer)"
    )

    @JvmSynthetic
    internal fun create(block: Block, context: BlockCreateContext): RebarBlock {
        schemaCache[block.position] = this
        return createConstructor.invoke(block, context) as RebarBlock
    }

    @JvmSynthetic
    internal fun load(block: Block, pdc: PersistentDataContainer): RebarBlock {
        schemaCache[block.position] = this
        return loadConstructor.invoke(block, pdc) as RebarBlock
    }

    fun <T> isType(clazz: Class<T>): Boolean {
        return clazz.isAssignableFrom(blockClass)
    }

    override fun getKey(): NamespacedKey = key

    override fun equals(other: Any?): Boolean = key == (other as? RebarBlockSchema)?.key

    override fun hashCode(): Int = key.hashCode()

    companion object {

        // This exists to avoid having to pass a key to the RebarBlock constructor
        internal val schemaCache: MutableMap<BlockPosition, RebarBlockSchema> = mutableMapOf()
    }
}
