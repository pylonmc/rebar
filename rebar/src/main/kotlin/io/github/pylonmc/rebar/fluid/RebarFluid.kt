package io.github.pylonmc.rebar.fluid

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.i18n.RebarTranslator.Companion.translator
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.getAddon
import io.github.pylonmc.rebar.util.rebarKey
import net.kyori.adventure.text.Component
import org.bukkit.Keyed
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack

/**
 * Fluids aren't necessarily just liquids, they can also be gases or other substances that can flow.
 *
 * @param material Used to display the fluid in tanks
 *
 * @see io.github.pylonmc.rebar.content.fluid.FluidPipe
 */
open class RebarFluid(
    private val key: NamespacedKey,
    val name: Component,
    material: Material,
    /**
     * @see RebarFluidTag
     */
    private val tags: MutableList<RebarFluidTag>,
) : Keyed {

    private val internalItem by lazy {
        val builder = ItemStackBuilder.of(material)
            .editPdc { it.set(rebarFluidKeyKey, RebarSerializers.NAMESPACED_KEY, key) }
            .addCustomModelDataString(key.toString())
            .name(name)

        for (tag in tags) {
            builder.lore(tag.displayText)
        }

        builder.lore(getAddon(key).footerName)

        builder.build()
    }

    val item
        get() = internalItem.clone()

    constructor(key: NamespacedKey, material: Material, vararg tags: RebarFluidTag) : this(
        key,
        Component.translatable("${key.namespace}.fluid.${key.key}"),
        material,
        tags.toMutableList()
    )

    init {
        val addon = RebarRegistry.ADDONS[NamespacedKey(key.namespace, key.namespace)]!!
        for (locale in addon.languages) {
            val translationKey = "${key.namespace}.fluid.${key.key}"
            if (!addon.translator.canTranslate(translationKey, locale)) {
                Rebar.logger.warning("${key.namespace} is missing a translation key for fluid ${key.key} (locale: ${locale.displayName} | expected translation key: $translationKey)")
            }
        }
    }

    fun addTag(tag: RebarFluidTag) = apply {
        check(!hasTag(tag.javaClass)) { "Fluid already has a tag of the same type" }
        tags.add(tag)
    }

    fun hasTag(type: Class<out RebarFluidTag>): Boolean
        = tags.any { type.isInstance(it) }

    inline fun <reified T: RebarFluidTag> hasTag(): Boolean
        = hasTag(T::class.java)

    /**
     * @throws IllegalArgumentException if the fluid does not have a tag of the given type
     */
    fun <T: RebarFluidTag> getTag(type: Class<T>): T
        = type.cast(tags.firstOrNull { type.isInstance(it) } ?: throw IllegalArgumentException("Fluid does not have a tag of type ${type.simpleName}"))

    inline fun <reified T: RebarFluidTag> getTag(): T
        = getTag(T::class.java)

    fun removeTag(tag: RebarFluidTag) {
        tags.remove(tag)
    }

    fun register() {
        RebarRegistry.FLUIDS.register(this)
    }

    override fun getKey(): NamespacedKey = key

    override fun equals(other: Any?): Boolean = other is RebarFluid && key == other.key
    override fun hashCode(): Int = key.hashCode()
    override fun toString(): String = key.toString()

    companion object {
        val rebarFluidKeyKey = rebarKey("rebar_fluid_key")

        /**
         * Get the fluid represented by the given item stack, or null if the stack is null, empty or does not represent a fluid.
         * See [item] for how to get an item stack that represents this fluid.
         */
        fun fromStack(stack: ItemStack?): RebarFluid? {
            if (stack == null || stack.isEmpty) return null
            val id = stack.persistentDataContainer.get(rebarFluidKeyKey, RebarSerializers.NAMESPACED_KEY) ?: return null
            return RebarRegistry.FLUIDS[id]
        }
    }
}