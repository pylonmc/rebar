package io.github.pylonmc.rebar.fluid

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.i18n.RebarTranslator.Companion.translator
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.getAddon
import io.github.pylonmc.rebar.util.rebarKey
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
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
    val color: TextColor,
    val nameWithoutColor: Component,
    material: Material,
    /**
     * @see RebarFluidProperty
     */
    private val properties: MutableList<RebarFluidProperty>,
) : Keyed {

    val addon = getAddon(key)

    val name = nameWithoutColor.color(color)

    private val internalItem by lazy {
        val builder = ItemStackBuilder.of(material)
            .editPdc { it.set(rebarFluidKeyKey, RebarSerializers.NAMESPACED_KEY, key) }
            .addCustomModelDataString(key.toString())
            .name(name)

        for (property in properties) {
            builder.lore(property.displayText)
        }

        builder.build()
    }

    val item
        get() = internalItem.clone()

    constructor(key: NamespacedKey, color: TextColor, material: Material, vararg properties: RebarFluidProperty) : this(
        key,
        color,
        Component.translatable("${key.namespace}.fluid.${key.key}"),
        material,
        properties.toMutableList()
    )

    init {
        if (key !in nameWarningsSuppressed) {
            for (locale in addon.translator.languages) {
                val translationKey = "${key.namespace}.fluid.${key.key}"
                if (!addon.translator.canTranslateExact(translationKey, locale)) {
                    Rebar.logger.warning("${key.namespace} is missing a translation key for fluid ${key.key} (locale: ${locale.displayName} | expected translation key: $translationKey)")
                }
            }
        }
    }

    fun addProperty(property: RebarFluidProperty) = apply {
        check(!hasProperty(property.javaClass)) { "Fluid already has a property of the same type" }
        properties.add(property)
    }

    fun hasProperty(type: Class<out RebarFluidProperty>): Boolean
        = properties.any { type.isInstance(it) }

    inline fun <reified T: RebarFluidProperty> hasProperty(): Boolean
        = hasProperty(T::class.java)

    /**
     * @throws IllegalArgumentException if the fluid does not have a property of the given type
     */
    fun <T: RebarFluidProperty> getProperty(type: Class<T>): T
        = type.cast(properties.firstOrNull { type.isInstance(it) } ?: throw IllegalArgumentException("Fluid does not have a property of type ${type.simpleName}"))

    inline fun <reified T: RebarFluidProperty> getProperty(): T
        = getProperty(T::class.java)

    fun removeProperty(property: RebarFluidProperty) {
        properties.remove(property)
    }

    fun register() {
        RebarRegistry.FLUIDS.register(this)
    }

    override fun getKey(): NamespacedKey = key

    override fun equals(other: Any?): Boolean = other is RebarFluid && key == other.key
    override fun hashCode(): Int = key.hashCode()
    override fun toString(): String = key.toString()

    companion object {
        private val nameWarningsSuppressed: MutableSet<NamespacedKey> = mutableSetOf()
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

        /**
         * Suppresses warnings about missing/incorrect translation keys for the fluid name
         * for the given fluid key
         */
        @JvmStatic
        fun suppressNameWarnings(key: NamespacedKey) {
            nameWarningsSuppressed.add(key)
        }
    }
}