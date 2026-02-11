package io.github.pylonmc.rebar.item

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.block.context.BlockCreateContext
import io.github.pylonmc.rebar.config.Config
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.config.Settings
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.entity.RebarEntity
import io.github.pylonmc.rebar.i18n.RebarArgument
import io.github.pylonmc.rebar.i18n.RebarTranslator.Companion.translator
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.papermc.paper.datacomponent.DataComponentTypes
import net.kyori.adventure.text.TranslatableComponent
import org.bukkit.Keyed
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.jetbrains.annotations.Contract

/**
 * RebarItems are wrappers around ItemStacks that allow you to easily add extra functionality.
 *
 * Unlike [RebarBlock] and [RebarEntity], RebarItem isn't persisted in memory, so you should
 * avoid storing any fields in your RebarItem classes. Instead, use the stack's [PersistentDataContainer]
 * to store data persistently.
 *
 * An implementation of RebarItem must have a constructor that takes an [ItemStack] as its only parameter.
 * This will be used to load an in-world ItemStack as this particular RebarItem class.
 */
open class RebarItem(val stack: ItemStack) : Keyed {

    private val key =
        stack.persistentDataContainer.get(RebarItemSchema.rebarItemKeyKey, RebarSerializers.NAMESPACED_KEY)!!

    /**
     * Additional data about the item's type.
     */
    val schema = RebarRegistry.ITEMS.getOrThrow(key)

    val researchBypassPermission = schema.researchBypassPermission
    val addon = schema.addon
    val rebarBlock = schema.rebarBlockKey
    val isDisabled = key in RebarConfig.DISABLED_ITEMS
    val research get() = schema.research

    /**
     * Returns settings associated with the item.
     *
     * Shorthand for `Settings.get(getKey())`
     */
    fun getSettings() = Settings.get(key)

    override fun equals(other: Any?): Boolean = key == (other as? RebarItem)?.key

    override fun hashCode(): Int = key.hashCode()

    override fun getKey(): NamespacedKey = key

    /**
     * Returns the list of placeholders to be substituted into the item's lore.
     *
     * Each placeholder is written as `%placeholder_name%` in the lore.
     */
    open fun getPlaceholders(): List<RebarArgument> = emptyList()

    /**
     * Places the block associated with this item, if it exists.
     */
    open fun place(context: BlockCreateContext): RebarBlock? = schema.place(context)

    companion object {

        private val nameWarningsSuppressed: MutableSet<NamespacedKey> = mutableSetOf()

        @Suppress("UnstableApiUsage")
        private fun checkName(schema: RebarItemSchema) {
            // Adventure is a perfect API with absolutely no problems whatsoever.
            val name = schema.getItemStack().getData(DataComponentTypes.ITEM_NAME) as? TranslatableComponent

            var isNameValid = true
            if (name == null || name.key() != ItemStackBuilder.nameKey(schema.key)) {
                Rebar.logger.warning("Item ${schema.key}'s name is not a translation key; check your item uses RebarItemStackBuilder.of(...)")
                isNameValid = false
            }

            if (isNameValid) {
                val translator = schema.addon.translator
                for (locale in schema.addon.languages) {
                    if (!translator.canTranslate(name!!.key(), locale)) {
                        Rebar.logger.warning(
                            "${schema.key.namespace} is missing a name translation key for item ${schema.key} (locale: ${locale.displayName} | expected translation key: ${
                                ItemStackBuilder.nameKey(
                                    schema.key
                                )
                            })"
                        )
                    }
                }
            }
        }

        private fun register(schema: RebarItemSchema) {
            if (schema.key !in nameWarningsSuppressed) {
                checkName(schema)
            }
            RebarRegistry.ITEMS.register(schema)

            // pre-merge configs and check for constructor errors
            fromStack(schema.getItemStack())
        }

        @JvmStatic
        @JvmOverloads
        fun register(itemClass: Class<out RebarItem>, template: ItemStack, rebarBlockKey: NamespacedKey? = null) =
            register(RebarItemSchema(itemClass, template, rebarBlockKey))

        inline fun <reified T: RebarItem>register(template: ItemStack, rebarBlockKey: NamespacedKey? = null) =
            register(T::class.java, template, rebarBlockKey)

        /**
         * Gets a RebarItem from an ItemStack if the item is a Rebar item
         * Returns null if the ItemStack is not a Rebar item
         */
        @JvmStatic
        @Contract("null -> null")
        fun fromStack(stack: ItemStack?): RebarItem? {
            if (stack == null || stack.isEmpty) return null
            val id = stack.persistentDataContainer.get(RebarItemSchema.rebarItemKeyKey, RebarSerializers.NAMESPACED_KEY)
                ?: return null
            val schema = RebarRegistry.ITEMS[id]
                ?: return null
            return schema.itemClass.cast(schema.loadConstructor.invoke(stack))
        }

        /**
         * Converts a regular ItemStack to a RebarItem of class [clazz]
         * Returns null if the ItemStack is not a Rebar item or is not of the specified [clazz]
         */
        @JvmStatic
        @Contract("null -> null")
        fun <T : RebarItem> fromStack(stack: ItemStack?, clazz: Class<T>): T? {
            val rebarItem = fromStack(stack) ?: return null
            if (!clazz.isInstance(rebarItem)) return null
            return clazz.cast(rebarItem)
        }

        @JvmSynthetic
        inline fun <reified T : RebarItem> from(stack: ItemStack?): T? {
            val rebarItem = fromStack(stack) ?: return null
            return rebarItem as? T
        }

        /**
         * Checks if [stack] is a Rebar item.
         */
        @JvmStatic
        @Contract("null -> false")
        fun isRebarItem(stack: ItemStack?): Boolean {
            return stack != null && stack.persistentDataContainer.has(RebarItemSchema.rebarItemKeyKey)
        }

        /**
         * Suppresses warnings about missing/incorrect translation keys for the item name and lore
         * for the given item key
         */
        @JvmStatic
        fun suppressNameWarnings(key: NamespacedKey) {
            nameWarningsSuppressed.add(key)
        }

        @JvmStatic
        fun getSettings(key: NamespacedKey): Config = Settings.get(key)
    }
}
