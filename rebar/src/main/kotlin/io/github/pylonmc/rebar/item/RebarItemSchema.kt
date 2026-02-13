package io.github.pylonmc.rebar.item

import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.block.context.BlockCreateContext
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.event.PreRebarBlockPlaceEvent
import io.github.pylonmc.rebar.item.research.Research
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.registry.RegistryHandler
import io.github.pylonmc.rebar.util.findConstructorMatching
import io.github.pylonmc.rebar.util.getAddon
import io.github.pylonmc.rebar.util.position.position
import io.github.pylonmc.rebar.util.rebarKey
import io.papermc.paper.command.brigadier.argument.ArgumentTypes.blockPosition
import org.bukkit.Keyed
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.jetbrains.annotations.Contract
import java.lang.invoke.MethodHandle

/**
 * Stores information about a Rebar item type, including its key, default [ItemStack], class, and
 * any associated blocks.
 *
 * You should not need to use this if you are not working on Rebar.
 */
class RebarItemSchema @JvmOverloads internal constructor(
    val itemClass: Class<out RebarItem>,
    private val template: ItemStack,
    val rebarBlockKey: NamespacedKey? = null
) : Keyed, RegistryHandler {

    private val key = template.persistentDataContainer.get(rebarItemKeyKey, RebarSerializers.NAMESPACED_KEY)
        ?: throw IllegalArgumentException("Provided item stack is not a Rebar item; make sure you are using ItemStackBuilder.defaultBuilder to create the item stack")

    val addon = getAddon(key)

    fun getItemStack(): ItemStack = template.clone()

    val research: Research?
        get() = RebarRegistry.RESEARCHES.find { key in it.unlocks }

    val researchBypassPermission = "rebar.item.${key.namespace}.${key.key}"

    @JvmSynthetic
    internal val loadConstructor: MethodHandle = itemClass.findConstructorMatching(ItemStack::class.java)
        ?: throw NoSuchMethodException("Item '$key' (${itemClass.simpleName}) is missing a load constructor (ItemStack)")

    fun prePlace(context: BlockCreateContext): Boolean {
        if (rebarBlockKey == null) {
            return false
        }
        val blockSchema = RebarRegistry.BLOCKS[rebarBlockKey]
        check(blockSchema != null) { "Block $rebarBlockKey not found" }
        check(template.type == blockSchema.material) {
            "Item $key places block $rebarBlockKey and so must have the same type - but the item is of type + ${template.type} while the block is of type ${blockSchema.material}"
        }
        if (BlockStorage.isRebarBlock(context.block)) { // special case: you can place on top of replaceable blocks
            return false
        }
        return PreRebarBlockPlaceEvent(context.block, blockSchema, context).callEvent()
    }

    /**
     * Attempts to place the block associated with this item.
     *
     * Does nothing if no block is associated with this item.
     */
    fun place(context: BlockCreateContext): RebarBlock {
        check(rebarBlockKey != null) { "Item $key does not place a block" }
        val blockSchema = RebarRegistry.BLOCKS[rebarBlockKey]
        check(blockSchema != null) { "Block $rebarBlockKey not found" }
        return BlockStorage.setBlock(context.block.position, blockSchema, context)
    }

    override fun getKey(): NamespacedKey = key

    override fun equals(other: Any?): Boolean = key == (other as? RebarItemSchema)?.key

    override fun hashCode(): Int = key.hashCode()

    companion object {
        val rebarItemKeyKey = rebarKey("rebar_item_key")

        @JvmStatic
        @Contract("null -> null")
        fun fromStack(stack: ItemStack?) : RebarItemSchema? {
            if (stack == null || stack.isEmpty) return null
            val id = stack.persistentDataContainer.get(rebarItemKeyKey, RebarSerializers.NAMESPACED_KEY)
                ?: return null
            return RebarRegistry.ITEMS[id]
        }
    }
}